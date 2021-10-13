/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType
import com.intellij.psi.impl.cache.TypeInfo
import com.intellij.psi.impl.compiled.ClsTypeElementImpl
import com.intellij.psi.impl.compiled.SignatureParsing
import com.intellij.psi.impl.compiled.StubBuildingVisitor
import org.jetbrains.kotlin.analysis.api.components.KtPsiTypeProvider
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.types.PublicTypeApproximator
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirModuleResolveState
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.withFirDeclaration
import org.jetbrains.kotlin.codegen.signature.BothSignatureWriter
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.jvm.jvmTypeMapper
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.utils.superConeTypes
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.substitution.AbstractConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.types.model.SimpleTypeMarker
import java.text.StringCharacterIterator

internal class KtFirPsiTypeProvider(
    override val analysisSession: KtFirAnalysisSession,
    override val token: ValidityToken,
) : KtPsiTypeProvider(), KtFirAnalysisSessionComponent {

    override fun asPsiType(
        type: KtType,
        context: PsiElement,
        mode: TypeMappingMode,
    ): PsiType? = withValidityAssertion {
        type.coneType.asPsiType(rootModuleSession, analysisSession.firResolveState, mode, context)
    }
}

private fun ConeKotlinType.simplifyType(session: FirSession, state: FirModuleResolveState): ConeKotlinType {
    val substitutor = AnonymousTypesSubstitutor(session, state)
    var currentType = this
    do {
        val oldType = currentType
        currentType = currentType.fullyExpandedType(session)
        currentType = currentType.upperBoundIfFlexible()
        currentType = substitutor.substituteOrSelf(currentType)
        currentType = PublicTypeApproximator.approximateTypeToPublicDenotable(currentType, session) ?: currentType
    } while (oldType !== currentType)
    return currentType
}

internal fun ConeKotlinType.asPsiType(
    session: FirSession,
    state: FirModuleResolveState,
    mode: TypeMappingMode,
    psiContext: PsiElement,
): PsiType? {
    val correctedType = simplifyType(session, state)

    if (correctedType is ConeClassErrorType || correctedType !is SimpleTypeMarker) return null

    if (correctedType.typeArguments.any { it is ConeClassErrorType }) return null

    val signatureWriter = BothSignatureWriter(BothSignatureWriter.Mode.SKIP_CHECKS)

    //TODO Check thread safety
    session.jvmTypeMapper.mapType(correctedType, mode, signatureWriter)

    val canonicalSignature = signatureWriter.toString()

    if (canonicalSignature.contains("L<error>")) return null

    require(!canonicalSignature.contains(SpecialNames.ANONYMOUS_STRING))

    val signature = StringCharacterIterator(canonicalSignature)
    val javaType = SignatureParsing.parseTypeString(signature, StubBuildingVisitor.GUESSING_MAPPER)
    val typeInfo = TypeInfo.fromString(javaType, false)
    val typeText = TypeInfo.createTypeText(typeInfo) ?: return null

    val typeElement = ClsTypeElementImpl(psiContext, typeText, '\u0000')
    return typeElement.type
}

private class AnonymousTypesSubstitutor(
    private val session: FirSession,
    private val state: FirModuleResolveState,
) : AbstractConeSubstitutor(session.typeContext) {
    override fun substituteType(type: ConeKotlinType): ConeKotlinType? {
        if (type !is ConeClassLikeType) return null

        val isAnonymous = type.classId.let { it?.shortClassName?.asString() == SpecialNames.ANONYMOUS_STRING }
        if (!isAnonymous) return null

        fun ConeClassLikeType.isNotInterface(): Boolean {
            val firClassNode = lookupTag.toSymbol(session)?.fir as? FirClass ?: return false
            return firClassNode.withFirDeclaration(state) { firSuperClass ->
                firSuperClass.classKind != ClassKind.INTERFACE
            }
        }

        val firClassNode = (type.lookupTag.toSymbol(session) as? FirClassSymbol)?.fir
        if (firClassNode != null) {
            val superTypesCones = firClassNode.withFirDeclaration(state, FirResolvePhase.SUPER_TYPES) {
                (it as? FirClass)?.superConeTypes
            }
            val superClass = superTypesCones?.firstOrNull { it.isNotInterface() }
            if (superClass != null) return superClass
        }

        return if (type.nullability.isNullable) session.builtinTypes.nullableAnyType.type
        else session.builtinTypes.anyType.type
    }
}
