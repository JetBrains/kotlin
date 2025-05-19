/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import com.intellij.psi.*
import com.intellij.psi.impl.compiled.ClsTypeElementImpl
import com.intellij.psi.impl.compiled.SignatureParsing
import com.intellij.psi.impl.compiled.StubBuildingVisitor
import org.jetbrains.kotlin.analysis.api.components.KaJavaInteroperabilityComponent
import org.jetbrains.kotlin.analysis.api.descriptors.KaFe10Session
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.KaFe10SessionComponent
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.getDescriptor
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.getSymbolDescriptor
import org.jetbrains.kotlin.analysis.api.descriptors.types.base.KaFe10Type
import org.jetbrains.kotlin.analysis.api.descriptors.utils.KaFe10JvmTypeMapperContext
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.impl.base.components.withPsiValidityAssertion
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeMappingMode
import org.jetbrains.kotlin.asJava.classes.annotateByKotlinType
import org.jetbrains.kotlin.codegen.signature.BothSignatureWriter
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.kotlin.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.platform.has
import org.jetbrains.kotlin.platform.jvm.JvmPlatform
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.resolve.jvm.annotations.hasJvmFieldAnnotation
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DescriptorWithContainerSource
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.model.SimpleTypeMarker
import org.jetbrains.org.objectweb.asm.Type

internal class KaFe10JavaInteroperabilityComponent(
    override val analysisSessionProvider: () -> KaFe10Session
) : KaBaseSessionComponent<KaFe10Session>(), KaJavaInteroperabilityComponent, KaFe10SessionComponent {
    private val typeMapper by lazy { KaFe10JvmTypeMapperContext(analysisContext.resolveSession) }

    override fun KaType.asPsiType(
        useSitePosition: PsiElement,
        allowErrorTypes: Boolean,
        mode: KaTypeMappingMode,
        isAnnotationMethod: Boolean,
        suppressWildcards: Boolean?,
        preserveAnnotations: Boolean,
        allowNonJvmPlatforms: Boolean,
    ): PsiType? = withPsiValidityAssertion(useSitePosition) {
        val kotlinType = (this as KaFe10Type).fe10Type

        with(typeMapper.typeContext) {
            if (kotlinType.contains { it.isError() }) {
                return null
            }
        }

        if (!analysisSession.useSiteModule.targetPlatform.has<JvmPlatform>()) return null

        val typeElement = asPsiTypeElement(
            simplifyType(kotlinType),
            useSitePosition,
            mode.toTypeMappingMode(this, isAnnotationMethod, suppressWildcards),
        )

        val psiType = typeElement?.type ?: return null
        if (!preserveAnnotations) return psiType

        return annotateByKotlinType(psiType, kotlinType, typeElement, inferNullability = true)
    }

    private fun KaTypeMappingMode.toTypeMappingMode(
        type: KaType,
        isAnnotationMethod: Boolean,
        suppressWildcards: Boolean?,
    ): TypeMappingMode {
        require(type is KaFe10Type)
        return when (this) {
            KaTypeMappingMode.DEFAULT -> TypeMappingMode.DEFAULT
            KaTypeMappingMode.DEFAULT_UAST -> TypeMappingMode.DEFAULT_UAST
            KaTypeMappingMode.GENERIC_ARGUMENT -> TypeMappingMode.GENERIC_ARGUMENT
            KaTypeMappingMode.SUPER_TYPE -> TypeMappingMode.SUPER_TYPE_AS_IS
            KaTypeMappingMode.SUPER_TYPE_KOTLIN_COLLECTIONS_AS_IS -> TypeMappingMode.SUPER_TYPE_KOTLIN_COLLECTIONS_AS_IS
            KaTypeMappingMode.RETURN_TYPE_BOXED -> TypeMappingMode.RETURN_TYPE_BOXED
            KaTypeMappingMode.RETURN_TYPE ->
                typeMapper.typeContext.getOptimalModeForReturnType(type.fe10Type, isAnnotationMethod)
            KaTypeMappingMode.VALUE_PARAMETER ->
                typeMapper.typeContext.getOptimalModeForValueParameter(type.fe10Type)
        }.let { typeMappingMode ->
            // Otherwise, i.e., if we won't skip type with no type arguments, flag overriding might bother a case like:
            // @JvmSuppressWildcards(false) Long -> java.lang.Long, not long, even though it should be no-op!
            if (type.fe10Type.arguments.isEmpty())
                typeMappingMode
            else
                typeMappingMode.updateArgumentModeFromAnnotations(
                    type.fe10Type,
                    typeMapper.typeContext,
                    suppressWildcards,
                )
        }
    }

    private fun simplifyType(type: UnwrappedType): KotlinType {
        var result = type
        do {
            val oldResult = result
            result = when (type) {
                is FlexibleType -> type.upperBound
                is DefinitelyNotNullType -> type.original
                else -> type
            }
        } while (result !== oldResult)
        return result
    }

    private fun asPsiTypeElement(type: KotlinType, useSitePosition: PsiElement, mode: TypeMappingMode): PsiTypeElement? {
        if (type !is SimpleTypeMarker) return null

        val signatureWriter = BothSignatureWriter(BothSignatureWriter.Mode.SKIP_CHECKS)
        typeMapper.mapType(type, mode, signatureWriter)

        val canonicalSignature = signatureWriter.toString()
        require(!canonicalSignature.contains(SpecialNames.ANONYMOUS_STRING))

        if (canonicalSignature.contains("L<error>")) return null
        if (canonicalSignature.contains(SpecialNames.NO_NAME_PROVIDED.asString())) return null

        val signature = SignatureParsing.CharIterator(canonicalSignature)
        val typeInfo = SignatureParsing.parseTypeStringToTypeInfo(signature, StubBuildingVisitor.GUESSING_PROVIDER)
        val typeText = typeInfo.text() ?: return null

        return SyntheticTypeElement(useSitePosition, typeText)
    }

    override fun PsiType.asKaType(useSitePosition: PsiElement): KaType? = withPsiValidityAssertion(useSitePosition) {
        throw UnsupportedOperationException("Conversion to KtType is not supported in K1 implementation")
    }

    override fun KaType.mapToJvmType(mode: TypeMappingMode): Type = withValidityAssertion {
        val kotlinType = (this as KaFe10Type).fe10Type
        return typeMapper.mapType(kotlinType, mode)
    }

    override val KaType.isPrimitiveBacked: Boolean
        get() = withValidityAssertion {
            val kotlinType = (this as KaFe10Type).fe10Type
            return typeMapper.isPrimitiveBacked(kotlinType)
        }

    override val PsiClass.namedClassSymbol: KaNamedClassSymbol?
        get() = withPsiValidityAssertion {
            return null /*TODO*/
        }

    override val PsiMember.callableSymbol: KaCallableSymbol?
        get() = withPsiValidityAssertion {
            return null /*TODO*/
        }

    override val KaCallableSymbol.containingJvmClassName: String?
        get() = withValidityAssertion {
            with(analysisSession) {
                val platform = containingModule.targetPlatform
                if (!platform.has<JvmPlatform>()) return null

                val containingSymbolOrSelf = computeContainingSymbolOrSelf(this@containingJvmClassName, useSiteSession)
                return when (val descriptor = containingSymbolOrSelf.getDescriptor()) {
                    is DescriptorWithContainerSource -> {
                        when (val containerSource = descriptor.containerSource) {
                            is FacadeClassSource -> containerSource.facadeClassName ?: containerSource.className
                            is KotlinJvmBinarySourceElement -> JvmClassName.byClassId(containerSource.binaryClass.classId)
                            else -> null
                        }?.fqNameForClassNameWithoutDollars?.asString()
                    }
                    else -> {
                        return if (containingSymbolOrSelf.isTopLevel) {
                            descriptor?.let(DescriptorToSourceUtils::getContainingFile)
                                ?.takeUnless { it.isScript() }
                                ?.javaFileFacadeFqName?.asString()
                        } else {
                            val classId = (containingSymbolOrSelf as? KaConstructorSymbol)?.containingClassId
                                ?: (containingSymbolOrSelf as? KaCallableSymbol)?.callableId?.classId
                            classId?.takeUnless { it.shortClassName.isSpecial }
                                ?.asFqNameString()
                        }
                    }
                }
            }
        }

    override val KaPropertySymbol.javaGetterName: Name
        get() = withValidityAssertion {
            val descriptor = getSymbolDescriptor(this) as? PropertyDescriptor
            if (descriptor is SyntheticJavaPropertyDescriptor) {
                return descriptor.getMethod.name
            }

            if (descriptor != null) {
                if (descriptor.hasJvmFieldAnnotation()) return descriptor.name

                val getter = descriptor.getter ?: return SpecialNames.NO_NAME_PROVIDED
                return Name.identifier(DescriptorUtils.getJvmName(getter) ?: JvmAbi.getterName(descriptor.name.asString()))
            }

            val ktPropertyName = (psi as? KtProperty)?.name ?: return SpecialNames.NO_NAME_PROVIDED
            return Name.identifier(JvmAbi.getterName(ktPropertyName))
        }

    override val KaPropertySymbol.javaSetterName: Name?
        get() = withValidityAssertion {
            val descriptor = getSymbolDescriptor(this) as? PropertyDescriptor
            if (descriptor is SyntheticJavaPropertyDescriptor) {
                return descriptor.setMethod?.name
            }

            if (descriptor != null) {
                if (!descriptor.isVar) {
                    return null
                }

                if (descriptor.hasJvmFieldAnnotation()) return descriptor.name

                val setter = descriptor.setter ?: return SpecialNames.NO_NAME_PROVIDED
                return Name.identifier(DescriptorUtils.getJvmName(setter) ?: JvmAbi.setterName(descriptor.name.asString()))
            }

            val ktPropertyName = (psi as? KtProperty)?.takeIf { it.isVar }?.name ?: return SpecialNames.NO_NAME_PROVIDED
            return Name.identifier(JvmAbi.setterName(ktPropertyName))
        }
}

private class SyntheticTypeElement(
    parent: PsiElement,
    typeText: String,
) : ClsTypeElementImpl(parent, typeText, '\u0000'), SyntheticElement