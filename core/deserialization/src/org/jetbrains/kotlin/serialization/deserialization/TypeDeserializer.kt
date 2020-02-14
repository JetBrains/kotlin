/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization.deserialization

import org.jetbrains.kotlin.builtins.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedAnnotations
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedTypeParameterDescriptor
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.builtIns
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.util.*

class TypeDeserializer(
    private val c: DeserializationContext,
    private val parent: TypeDeserializer?,
    typeParameterProtos: List<ProtoBuf.TypeParameter>,
    private val debugName: String,
    private val containerPresentableName: String,
    var experimentalSuspendFunctionTypeEncountered: Boolean = false
) {
    private val classDescriptors: (Int) -> ClassDescriptor? = c.storageManager.createMemoizedFunctionWithNullableValues { fqNameIndex ->
        computeClassDescriptor(fqNameIndex)
    }

    private val typeAliasDescriptors: (Int) -> ClassifierDescriptor? =
        c.storageManager.createMemoizedFunctionWithNullableValues { fqNameIndex ->
            computeTypeAliasDescriptor(fqNameIndex)
        }

    private val typeParameterDescriptors =
        if (typeParameterProtos.isEmpty()) {
            mapOf<Int, TypeParameterDescriptor>()
        } else {
            val result = LinkedHashMap<Int, TypeParameterDescriptor>()
            for ((index, proto) in typeParameterProtos.withIndex()) {
                result[proto.id] = DeserializedTypeParameterDescriptor(c, proto, index)
            }
            result
        }

    val ownTypeParameters: List<TypeParameterDescriptor>
        get() = typeParameterDescriptors.values.toList()

    // TODO: don't load identical types from TypeTable more than once
    fun type(proto: ProtoBuf.Type): KotlinType {
        if (proto.hasFlexibleTypeCapabilitiesId()) {
            val id = c.nameResolver.getString(proto.flexibleTypeCapabilitiesId)
            val lowerBound = simpleType(proto)
            val upperBound = simpleType(proto.flexibleUpperBound(c.typeTable)!!)
            return c.components.flexibleTypeDeserializer.create(proto, id, lowerBound, upperBound)
        }

        return simpleType(proto)
    }

    fun simpleType(proto: ProtoBuf.Type): SimpleType {
        val localClassifierType = when {
            proto.hasClassName() -> computeLocalClassifierReplacementType(proto.className)
            proto.hasTypeAliasName() -> computeLocalClassifierReplacementType(proto.typeAliasName)
            else -> null
        }

        if (localClassifierType != null) return localClassifierType

        val constructor = typeConstructor(proto)
        if (ErrorUtils.isError(constructor.declarationDescriptor)) {
            return ErrorUtils.createErrorTypeWithCustomConstructor(constructor.toString(), constructor)
        }

        val annotations = DeserializedAnnotations(c.storageManager) {
            c.components.annotationAndConstantLoader.loadTypeAnnotations(proto, c.nameResolver)
        }

        fun ProtoBuf.Type.collectAllArguments(): List<ProtoBuf.Type.Argument> =
            argumentList + outerType(c.typeTable)?.collectAllArguments().orEmpty()

        val arguments = proto.collectAllArguments().mapIndexed { index, argumentProto ->
            typeArgument(constructor.parameters.getOrNull(index), argumentProto)
        }.toList()

        val simpleType = if (Flags.SUSPEND_TYPE.get(proto.flags)) {
            createSuspendFunctionType(annotations, constructor, arguments, proto.nullable)
        } else {
            KotlinTypeFactory.simpleType(annotations, constructor, arguments, proto.nullable)
        }

        val computedType = proto.abbreviatedType(c.typeTable)?.let {
            simpleType.withAbbreviation(simpleType(it))
        } ?: simpleType

        // TODO: move this hack in some platform specific place ASAP
        if (proto.hasClassName()) {
            val classId = c.nameResolver.getClassId(proto.className)
            val originalPackageFqn = classId.packageFqName
            if (originalPackageFqn in forwardPackagesSet) {
                // This hack is about keeping original class id written into proto which is required for correct IR linkage
                val classDescriptor = constructor.declarationDescriptor as ClassDescriptor
                val realPackageFqn = (classDescriptor.containingDeclaration as PackageFragmentDescriptor).fqName
                if (originalPackageFqn != realPackageFqn) {
                    return SupposititiousSimpleType(computedType, classId)
                }
            }
        }

        return computedType
    }

    private fun typeConstructor(proto: ProtoBuf.Type): TypeConstructor {
        fun notFoundClass(classIdIndex: Int): ClassDescriptor {
            val classId = c.nameResolver.getClassId(classIdIndex)
            val typeParametersCount = generateSequence(proto) { it.outerType(c.typeTable) }.map { it.argumentCount }.toMutableList()
            val classNestingLevel = generateSequence(classId, ClassId::getOuterClassId).count()
            while (typeParametersCount.size < classNestingLevel) {
                typeParametersCount.add(0)
            }
            return c.components.notFoundClasses.getClass(classId, typeParametersCount)
        }

        return when {
            proto.hasClassName() -> (classDescriptors(proto.className) ?: notFoundClass(proto.className)).typeConstructor
            proto.hasTypeParameter() ->
                typeParameterTypeConstructor(proto.typeParameter)
                    ?: ErrorUtils.createErrorTypeConstructor(
                        "Unknown type parameter ${proto.typeParameter}. Please try recompiling module containing \"$containerPresentableName\""
                    )
            proto.hasTypeParameterName() -> {
                val container = c.containingDeclaration
                val name = c.nameResolver.getString(proto.typeParameterName)
                val parameter = ownTypeParameters.find { it.name.asString() == name }
                parameter?.typeConstructor ?: ErrorUtils.createErrorTypeConstructor("Deserialized type parameter $name in $container")
            }
            proto.hasTypeAliasName() -> (typeAliasDescriptors(proto.typeAliasName) ?: notFoundClass(proto.typeAliasName)).typeConstructor
            else -> ErrorUtils.createErrorTypeConstructor("Unknown type")
        }
    }

    private fun createSuspendFunctionType(
        annotations: Annotations,
        functionTypeConstructor: TypeConstructor,
        arguments: List<TypeProjection>,
        nullable: Boolean
    ): SimpleType {
        val result = when (functionTypeConstructor.parameters.size - arguments.size) {
            0 -> createSuspendFunctionTypeForBasicCase(annotations, functionTypeConstructor, arguments, nullable)
            // This case for types written by eap compiler 1.1
            1 -> {
                val arity = arguments.size - 1
                if (arity >= 0) {
                    KotlinTypeFactory.simpleType(
                        annotations,
                        functionTypeConstructor.builtIns.getSuspendFunction(arity).typeConstructor,
                        arguments,
                        nullable
                    )
                } else {
                    null
                }
            }
            else -> null
        }
        return result ?: ErrorUtils.createErrorTypeWithArguments(
            "Bad suspend function in metadata with constructor: $functionTypeConstructor",
            arguments
        )
    }

    private fun createSuspendFunctionTypeForBasicCase(
        annotations: Annotations,
        functionTypeConstructor: TypeConstructor,
        arguments: List<TypeProjection>,
        nullable: Boolean
    ): SimpleType? {
        val functionType = KotlinTypeFactory.simpleType(annotations, functionTypeConstructor, arguments, nullable)
        return if (!functionType.isFunctionType) null
        else transformRuntimeFunctionTypeToSuspendFunction(functionType)
    }

    private fun transformRuntimeFunctionTypeToSuspendFunction(funType: KotlinType): SimpleType? {
        val isReleaseCoroutines = c.components.configuration.releaseCoroutines

        val continuationArgumentType = funType.getValueParameterTypesFromFunctionType().lastOrNull()?.type ?: return null
        val continuationArgumentFqName = continuationArgumentType.constructor.declarationDescriptor?.fqNameSafe
        if (continuationArgumentType.arguments.size != 1 || !(isContinuation(continuationArgumentFqName, true) ||
                    isContinuation(continuationArgumentFqName, false))
        ) {
            return funType as SimpleType?
        }

        val suspendReturnType = continuationArgumentType.arguments.single().type

        // Load kotlin.suspend as accepting and returning suspend function type independent of its version requirement
        if (c.containingDeclaration.safeAs<CallableDescriptor>()?.fqNameOrNull() == KOTLIN_SUSPEND_BUILT_IN_FUNCTION_FQ_NAME) {
            return createSimpleSuspendFunctionType(funType, suspendReturnType)
        }

        // Load experimental suspend function type as suspend function type
        experimentalSuspendFunctionTypeEncountered = experimentalSuspendFunctionTypeEncountered ||
                (isReleaseCoroutines && isContinuation(continuationArgumentFqName, !isReleaseCoroutines))

        return createSimpleSuspendFunctionType(funType, suspendReturnType)
    }

    private fun createSimpleSuspendFunctionType(
        funType: KotlinType,
        suspendReturnType: KotlinType
    ): SimpleType {
        return createFunctionType(
            funType.builtIns,
            funType.annotations,
            funType.getReceiverTypeFromFunctionType(),
            funType.getValueParameterTypesFromFunctionType().dropLast(1).map(TypeProjection::getType),
            // TODO: names
            null,
            suspendReturnType,
            suspendFunction = true
        ).makeNullableAsSpecified(funType.isMarkedNullable)
    }

    private fun typeParameterTypeConstructor(typeParameterId: Int): TypeConstructor? =
        typeParameterDescriptors[typeParameterId]?.typeConstructor ?: parent?.typeParameterTypeConstructor(typeParameterId)

    private fun computeClassDescriptor(fqNameIndex: Int): ClassDescriptor? {
        val id = c.nameResolver.getClassId(fqNameIndex)
        if (id.isLocal) {
            // Local classes can't be found in scopes
            return c.components.deserializeClass(id)
        }
        return c.components.moduleDescriptor.findClassAcrossModuleDependencies(id)
    }

    private fun computeLocalClassifierReplacementType(className: Int): SimpleType? {
        if (c.nameResolver.getClassId(className).isLocal) {
            return c.components.localClassifierTypeSettings.replacementTypeForLocalClassifiers
        }
        return null
    }

    private fun computeTypeAliasDescriptor(fqNameIndex: Int): ClassifierDescriptor? {
        val id = c.nameResolver.getClassId(fqNameIndex)
        return if (id.isLocal) {
            // TODO: support deserialization of local type aliases (see KT-13692)
            return null
        } else {
            c.components.moduleDescriptor.findTypeAliasAcrossModuleDependencies(id)
        }
    }

    private fun typeArgument(parameter: TypeParameterDescriptor?, typeArgumentProto: ProtoBuf.Type.Argument): TypeProjection {
        if (typeArgumentProto.projection == ProtoBuf.Type.Argument.Projection.STAR) {
            return if (parameter == null)
                StarProjectionForAbsentTypeParameter(c.components.moduleDescriptor.builtIns)
            else
                StarProjectionImpl(parameter)
        }

        val projection = ProtoEnumFlags.variance(typeArgumentProto.projection)
        val type = typeArgumentProto.type(c.typeTable) ?: return TypeProjectionImpl(ErrorUtils.createErrorType("No type recorded"))

        return TypeProjectionImpl(projection, type(type))
    }

    override fun toString() = debugName + (if (parent == null) "" else ". Child of ${parent.debugName}")

    companion object {
        private val cNames = FqName("cnames")
        private val cNamesStructs = cNames.child(Name.identifier("structs"))

        private val objCNames = FqName("objcnames")
        private val objCNamesClasses = objCNames.child(Name.identifier("classes"))
        private val objCNamesProtocols = objCNames.child(Name.identifier("protocols"))

        private val forwardPackagesSet = setOf(cNamesStructs, objCNamesClasses, objCNamesProtocols)
    }
}
