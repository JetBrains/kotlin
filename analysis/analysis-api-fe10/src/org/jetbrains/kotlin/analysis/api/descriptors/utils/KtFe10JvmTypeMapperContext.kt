/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.utils

import org.jetbrains.kotlin.builtins.functions.BuiltInFunctionArity
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.builtins.isKFunctionType
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.codegen.JvmCodegenUtil
import org.jetbrains.kotlin.codegen.signature.JvmSignatureWriter
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper.Companion.hasNothingInNonContravariantPosition
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.error.ErrorUtils
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.org.objectweb.asm.Type

internal class KtFe10JvmTypeMapperContext(private val resolveSession: ResolveSession) : TypeMappingContext<JvmSignatureWriter> {
    companion object {
        fun getNestedType(type: KotlinType): NestedType {
            val possiblyInnerType = type.buildPossiblyInnerType() ?: throw IllegalArgumentException(type.toString())
            val innerTypesAsList = possiblyInnerType.segments()
            val indexOfParameterizedType = innerTypesAsList.indexOfFirst { innerPart -> innerPart.arguments.isNotEmpty() }

            return if (indexOfParameterizedType < 0 || innerTypesAsList.size == 1) {
                val classifier = type.constructor.declarationDescriptor as? ClassifierDescriptorWithTypeParameters
                    ?: throw IllegalArgumentException(type.toString())

                NestedType(PossiblyInnerType(classifier, type.arguments, null), emptyList())
            } else {
                NestedType(innerTypesAsList[indexOfParameterizedType], innerTypesAsList.drop(indexOfParameterizedType + 1))
            }
        }
    }

    class NestedType(val root: PossiblyInnerType, val nested: List<PossiblyInnerType>) {
        val allInnerTypes: List<PossiblyInnerType>
            get() = buildList {
                add(root)
                addAll(nested)
            }
    }

    override val typeContext = KtFe10TypeSystemCommonBackendContextForTypeMapping(resolveSession)

    fun mapType(type: KotlinType, mode: TypeMappingMode = TypeMappingMode.DEFAULT, sw: JvmSignatureWriter? = null): Type {
        return AbstractTypeMapper.mapType(this, type, mode, sw)
    }

    fun isPrimitiveBacked(type: KotlinType): Boolean =
        AbstractTypeMapper.isPrimitiveBacked(this, type)

    override fun getClassInternalName(typeConstructor: TypeConstructorMarker): String {
        require(typeConstructor is TypeConstructor)

        return when (val declaration = typeConstructor.declarationDescriptor) {
            is TypeParameterDescriptor -> declaration.name.asString().sanitize()
            is TypeAliasDescriptor -> getClassInternalName(declaration.expandedType.constructor)
            is ClassDescriptor -> computeClassInternalName(declaration) ?: computeSupertypeInternalName(declaration)
            else -> error("Unexpected declaration type: $declaration")
        }
    }

    private fun computeClassInternalName(descriptor: ClassDescriptor): String? {
        val selfName = descriptor.name.takeIf { !it.isSpecial } ?: return null

        return when (val parent = descriptor.containingDeclaration) {
            is PackageFragmentDescriptor -> {
                val packageInternalName = parent.fqName.asString().replace('.', '/')
                "$packageInternalName/$selfName"
            }
            is ClassDescriptor -> {
                val parentInternalName = computeClassInternalName(parent)
                if (parentInternalName != null) "$parentInternalName$$selfName" else null
            }
            else -> selfName.asString()
        }
    }

    private fun computeSupertypeInternalName(descriptor: ClassDescriptor): String {
        var interfaceSupertypeInternalName: String? = null

        for (supertype in descriptor.typeConstructor.supertypes) {
            val declaration = supertype.constructor.declarationDescriptor ?: continue

            if (declaration !is ClassDescriptor || declaration.name.isSpecial) {
                continue
            }

            if (declaration.kind != ClassKind.INTERFACE && declaration.kind != ClassKind.ANNOTATION_CLASS) {
                val internalName = computeClassInternalName(declaration)
                if (internalName != null) {
                    return internalName
                }
            } else if (interfaceSupertypeInternalName == null) {
                interfaceSupertypeInternalName = computeClassInternalName(declaration)
            }
        }

        return interfaceSupertypeInternalName ?: "java/lang/Object"
    }

    override fun getScriptInternalName(typeConstructor: TypeConstructorMarker): String {
        return getClassInternalName(typeConstructor)
    }

    override fun JvmSignatureWriter.writeGenericType(type: KotlinTypeMarker, asmType: Type, mode: TypeMappingMode) {
        require(type is KotlinType)

        val typeDeclaration = type.constructor.declarationDescriptor

        val skipArguments = skipGenericSignature()
                || typeContext.hasNothingInNonContravariantPosition(type)
                || type.arguments.isEmpty()
                || typeDeclaration == null
                || ErrorUtils.isError(typeDeclaration)

        if (skipArguments) {
            writeAsmType(asmType)
            return
        }

        val nestedType = getNestedType(type)
        if (nestedType.nested.isEmpty()) {
            writeClassBegin(asmType)
            writeGenericArguments(this, nestedType.root, mode)
        } else {
            writeOuterClassBegin(asmType, mapType(nestedType.root.classDescriptor.defaultType).internalName)
            writeGenericArguments(this, nestedType.root, mode)
            writeInnerParts(nestedType.nested, this, mode, 0)
        }

        writeClassEnd()
    }

    private fun writeGenericArguments(sw: JvmSignatureWriter, type: PossiblyInnerType, mode: TypeMappingMode) {
        val classifier = type.classifierDescriptor
        val defaultType = classifier.defaultType
        val parameters = classifier.declaredTypeParameters
        val arguments = type.arguments

        if ((defaultType.isFunctionType && arguments.size > BuiltInFunctionArity.BIG_ARITY) || defaultType.isKFunctionType) {
            writeGenericArguments(sw, arguments.take(1), parameters.take(1), mode)
            return
        }

        writeGenericArguments(sw, arguments, parameters, mode)
    }

    private fun writeGenericArguments(
        sw: JvmSignatureWriter,
        arguments: List<TypeProjection>,
        parameters: List<TypeParameterDescriptor>,
        mode: TypeMappingMode
    ) {
        with(KotlinTypeMapper) {
            typeContext.writeGenericArguments(sw, arguments, parameters, mode) { type, sw, mode ->
                mapType(type as KotlinType, mode, sw)
            }
        }
    }

    private fun writeInnerParts(innerTypesAsList: List<PossiblyInnerType>, sw: JvmSignatureWriter, mode: TypeMappingMode, index: Int) {
        for (innerPart in innerTypesAsList.subList(index, innerTypesAsList.size)) {
            sw.writeInnerClass(getJvmShortName(innerPart.classDescriptor))
            writeGenericArguments(sw, innerPart, mode)
        }
    }

    private fun getJvmShortName(declaration: ClassDescriptor): String {
        if (!DescriptorUtils.isLocal(declaration)) {
            val shortClassName = JavaToKotlinClassMap.mapKotlinToJava(declaration.fqNameUnsafe)?.shortClassName?.asString()
            if (shortClassName != null) {
                return shortClassName
            }
        }

        return SpecialNames.safeIdentifier(declaration.name).identifier
    }

    private fun String.sanitize() = JvmCodegenUtil.sanitizeNameIfNeeded(this, resolveSession.languageVersionSettings)
}