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

import org.jetbrains.kotlin.builtins.functions.BuiltInFictitiousFunctionClassFactory
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationWithTarget
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.serialization.Flags
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedAnnotationsWithPossibleTargets
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedTypeParameterDescriptor
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.utils.toReadOnlyList
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
            get() = typeParameterDescriptors.values.toReadOnlyList()

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
            c.components.annotationAndConstantLoader
                    .loadTypeAnnotations(proto, c.nameResolver)
                    .map { AnnotationWithTarget(it, null) } + additionalAnnotations.getAllAnnotations()
        }

        fun ProtoBuf.Type.collectAllArguments(): List<ProtoBuf.Type.Argument> =
                argumentList + outerType(c.typeTable)?.collectAllArguments().orEmpty()

        val arguments = proto.collectAllArguments().mapIndexed { index, proto ->
            typeArgument(constructor.parameters.getOrNull(index), proto)
        }.toReadOnlyList()

        val simpleType = KotlinTypeFactory.simpleType(annotations, constructor, arguments, proto.nullable)

        val abbreviatedTypeProto = proto.abbreviatedType(c.typeTable) ?: return simpleType
        return simpleType.withAbbreviation(simpleType(abbreviatedTypeProto, additionalAnnotations))
    }

    private fun typeConstructor(proto: ProtoBuf.Type): TypeConstructor =
            when {
                proto.hasClassName() -> {
                    if (Flags.SUSPEND_TYPE.get(proto.flags)) {
                        getSuspendFunctionTypeConstructor(proto)
                    }
                    else {
                        classDescriptors(proto.className)?.typeConstructor
                        ?: c.components.notFoundClasses.getClass(proto, c.nameResolver, c.typeTable)
                    }
                }
                proto.hasTypeParameter() ->
                    typeParameterTypeConstructor(proto.typeParameter)
                    ?: ErrorUtils.createErrorTypeConstructor("Unknown type parameter ${proto.typeParameter}")
                proto.hasTypeParameterName() -> {
                    val container = c.containingDeclaration
                    val name = c.nameResolver.getString(proto.typeParameterName)
                    val parameter = ownTypeParameters.find { it.name.asString() == name }
                    parameter?.typeConstructor ?: ErrorUtils.createErrorTypeConstructor("Deserialized type parameter $name in $container")
                }
                proto.hasTypeAliasName() -> {
                    typeAliasDescriptors(proto.typeAliasName)?.typeConstructor
                    ?: c.components.notFoundClasses.getTypeAlias(proto, c.nameResolver, c.typeTable)
                }
                else -> ErrorUtils.createErrorTypeConstructor("Unknown type")
            }

    private fun getSuspendFunctionTypeConstructor(proto: ProtoBuf.Type): TypeConstructor {
        val classId = c.nameResolver.getClassId(proto.className)
        val arity = BuiltInFictitiousFunctionClassFactory.getFunctionalClassArity(classId.shortClassName.asString(), classId.packageFqName)

        return if (arity != null)
            c.containingDeclaration.builtIns.getSuspendFunction(arity - 1).typeConstructor
        else
            ErrorUtils.createErrorTypeConstructor("Class is not a FunctionN ${classId.asString()}")
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
