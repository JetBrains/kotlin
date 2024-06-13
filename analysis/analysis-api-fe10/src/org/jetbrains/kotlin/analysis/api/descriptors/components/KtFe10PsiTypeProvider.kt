/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypeElement
import com.intellij.psi.SyntheticElement
import com.intellij.psi.impl.compiled.ClsTypeElementImpl
import com.intellij.psi.impl.compiled.SignatureParsing
import com.intellij.psi.impl.compiled.StubBuildingVisitor
import org.jetbrains.kotlin.analysis.api.components.KaPsiTypeProvider
import org.jetbrains.kotlin.analysis.api.descriptors.KaFe10Session
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.KaFe10SessionComponent
import org.jetbrains.kotlin.analysis.api.descriptors.types.base.KaFe10Type
import org.jetbrains.kotlin.analysis.api.descriptors.utils.KaFe10JvmTypeMapperContext
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeMappingMode
import org.jetbrains.kotlin.asJava.classes.annotateByKotlinType
import org.jetbrains.kotlin.codegen.signature.BothSignatureWriter
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.load.kotlin.getOptimalModeForReturnType
import org.jetbrains.kotlin.load.kotlin.getOptimalModeForValueParameter
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.platform.has
import org.jetbrains.kotlin.platform.jvm.JvmPlatform
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.model.SimpleTypeMarker
import java.lang.UnsupportedOperationException

internal class KaFe10PsiTypeProvider(
    override val analysisSession: KaFe10Session
) : KaPsiTypeProvider(), KaFe10SessionComponent {
    override val token: KaLifetimeToken
        get() = analysisSession.token

    private val typeMapper by lazy { KaFe10JvmTypeMapperContext(analysisContext.resolveSession) }

    override fun asPsiType(
        type: KaType,
        useSitePosition: PsiElement,
        allowErrorTypes: Boolean,
        mode: KaTypeMappingMode,
        isAnnotationMethod: Boolean,
        suppressWildcards: Boolean?,
        preserveAnnotations: Boolean,
    ): PsiType? {
        val kotlinType = (type as KaFe10Type).fe10Type

        with(typeMapper.typeContext) {
            if (kotlinType.contains { it.isError() }) {
                return null
            }
        }

        if (!analysisSession.useSiteModule.platform.has<JvmPlatform>()) return null

        val typeElement = asPsiTypeElement(
            simplifyType(kotlinType),
            useSitePosition,
            mode.toTypeMappingMode(type, isAnnotationMethod, suppressWildcards),
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
            KaTypeMappingMode.SUPER_TYPE -> TypeMappingMode.SUPER_TYPE
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

    override fun asKaType(
        psiType: PsiType,
        useSitePosition: PsiElement,
    ): KaType? {
        throw UnsupportedOperationException("Conversion to KtType is not supported in K1 implementation")
    }
}

private class SyntheticTypeElement(parent: PsiElement, typeText: String) : ClsTypeElementImpl(parent, typeText, '\u0000'), SyntheticElement