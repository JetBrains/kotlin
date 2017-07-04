/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.serialization.deserialization

import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.builtins.transformRuntimeFunctionTypeToSuspendFunction
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationWithTarget
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.serialization.Flags
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedAnnotationsWithPossibleTargets
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedTypeParameterDescriptor
import org.jetbrains.kotlin.types.*
import java.util.*

class TypeDeserializer(
        private val c: DeserializationContext,
        private val parent: TypeDeserializer?,
        typeParameterProtos: List<ProtoBuf.TypeParameter>,
        private val debugName: String
) {
    private val classDescriptors: (Int) -> ClassDescriptor? = c.storageManager.createMemoizedFunctionWithNullableValues {
        fqNameIndex -> computeClassDescriptor(fqNameIndex)
    }

    private val typeAliasDescriptors: (Int) -> ClassifierDescriptor? = c.storageManager.createMemoizedFunctionWithNullableValues {
        fqNameIndex -> computeTypeAliasDescriptor(fqNameIndex)
    }

    private val typeParameterDescriptors =
        if (typeParameterProtos.isEmpty()) {
            mapOf<Int, TypeParameterDescriptor>()
        }
        else {
            val result = LinkedHashMap<Int, TypeParameterDescriptor>()
            for ((index, proto) in typeParameterProtos.withIndex()) {
                result[proto.id] = DeserializedTypeParameterDescriptor(c, proto, index)
            }
            result
        }

    val ownTypeParameters: List<TypeParameterDescriptor>
            get() = typeParameterDescriptors.values.toList()

    // TODO: don't load identical types from TypeTable more than once
    fun type(proto: ProtoBuf.Type, additionalAnnotations: Annotations = Annotations.EMPTY): KotlinType {
        if (proto.hasFlexibleTypeCapabilitiesId()) {
            val id = c.nameResolver.getString(proto.flexibleTypeCapabilitiesId)
            val lowerBound = simpleType(proto, additionalAnnotations)
            val upperBound = simpleType(proto.flexibleUpperBound(c.typeTable)!!, additionalAnnotations)
            return c.components.flexibleTypeDeserializer.create(proto, id, lowerBound, upperBound)
        }

        return simpleType(proto, additionalAnnotations)
    }

    fun simpleType(proto: ProtoBuf.Type, additionalAnnotations: Annotations = Annotations.EMPTY): SimpleType {
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

        val annotations = DeserializedAnnotationsWithPossibleTargets(c.storageManager) {
            c.components.annotationAndConstantLoader.loadTypeAnnotations(proto, c.nameResolver)
                    .map { AnnotationWithTarget(it, null) }
                    .plus(additionalAnnotations.getAllAnnotations())
                    .toList()
        }

        fun ProtoBuf.Type.collectAllArguments(): List<ProtoBuf.Type.Argument> =
                argumentList + outerType(c.typeTable)?.collectAllArguments().orEmpty()

        val arguments = proto.collectAllArguments().mapIndexed { index, proto ->
            typeArgument(constructor.parameters.getOrNull(index), proto)
        }.toList()

        val simpleType = if (Flags.SUSPEND_TYPE.get(proto.flags)) {
            createSuspendFunctionType(annotations, constructor, arguments, proto.nullable)
        }
        else {
            KotlinTypeFactory.simpleType(annotations, constructor, arguments, proto.nullable)
        }

        val abbreviatedTypeProto = proto.abbreviatedType(c.typeTable) ?: return simpleType
        return simpleType.withAbbreviation(simpleType(abbreviatedTypeProto, additionalAnnotations))
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
                ?: ErrorUtils.createErrorTypeConstructor("Unknown type parameter ${proto.typeParameter}")
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
            0 -> {
                val functionType = KotlinTypeFactory.simpleType(annotations, functionTypeConstructor, arguments, nullable)
                functionType.takeIf { it.isFunctionType }?.let(::transformRuntimeFunctionTypeToSuspendFunction)
            }
            // This case for types written by eap compiler 1.1
            1 -> {
                val arity = arguments.size - 1
                if (arity >= 0) {
                    KotlinTypeFactory.simpleType(annotations, functionTypeConstructor.builtIns.getSuspendFunction(arity).typeConstructor, arguments, nullable)
                }
                else {
                    null
                }
            }
            else -> null
        }
        return result ?: ErrorUtils.createErrorTypeWithArguments("Bad suspend function in metadata with constructor: $functionTypeConstructor", arguments)
    }

    private fun typeParameterTypeConstructor(typeParameterId: Int): TypeConstructor? =
            typeParameterDescriptors.get(typeParameterId)?.typeConstructor ?:
            parent?.typeParameterTypeConstructor(typeParameterId)

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
        }
        else {
            c.components.moduleDescriptor.findTypeAliasAcrossModuleDependencies(id)
        }
    }

    private fun typeArgument(parameter: TypeParameterDescriptor?, typeArgumentProto: ProtoBuf.Type.Argument): TypeProjection {
        if (typeArgumentProto.projection == ProtoBuf.Type.Argument.Projection.STAR) {
            return if (parameter == null)
                TypeBasedStarProjectionImpl(c.components.moduleDescriptor.builtIns.nullableAnyType)
            else
                StarProjectionImpl(parameter)
        }

        val variance = Deserialization.variance(typeArgumentProto.projection)
        val type = typeArgumentProto.type(c.typeTable) ?:
                return TypeProjectionImpl(ErrorUtils.createErrorType("No type recorded"))

        return TypeProjectionImpl(variance, type(type))
    }

    override fun toString() = debugName + (if (parent == null) "" else ". Child of ${parent.debugName}")
}
