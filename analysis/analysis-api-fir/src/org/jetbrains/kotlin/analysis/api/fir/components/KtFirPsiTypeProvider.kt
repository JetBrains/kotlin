/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.lang.java.JavaLanguage
import com.intellij.psi.*
import com.intellij.psi.impl.cache.TypeInfo
import com.intellij.psi.impl.compiled.ClsTypeElementImpl
import com.intellij.psi.impl.compiled.SignatureParsing
import com.intellij.psi.impl.compiled.StubBuildingVisitor
import org.jetbrains.kotlin.analysis.api.components.KtPsiTypeProvider
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.types.KtFirType
import org.jetbrains.kotlin.analysis.api.fir.types.PublicTypeApproximator
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.types.KtTypeMappingMode
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.codegen.signature.BothSignatureWriter
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.java.JavaVisibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClassSymbol
import org.jetbrains.kotlin.fir.backend.jvm.jvmTypeMapper
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.substitution.AbstractConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.load.kotlin.getOptimalModeForReturnType
import org.jetbrains.kotlin.load.kotlin.getOptimalModeForValueParameter
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.types.model.SimpleTypeMarker
import java.text.StringCharacterIterator

internal class KtFirPsiTypeProvider(
    override val analysisSession: KtFirAnalysisSession,
    override val token: ValidityToken,
) : KtPsiTypeProvider(), KtFirAnalysisSessionComponent {

    override fun asPsiType(
        type: KtType,
        useSitePosition: PsiElement,
        mode: KtTypeMappingMode,
        isAnnotationMethod: Boolean,
    ): PsiType? = withValidityAssertion {
        type.coneType.asPsiType(
            rootModuleSession,
            mode.toTypeMappingMode(type, isAnnotationMethod),
            useSitePosition
        )
    }

    private fun KtTypeMappingMode.toTypeMappingMode(type: KtType, isAnnotationMethod: Boolean): TypeMappingMode {
        require(type is KtFirType)
        return when (this) {
            KtTypeMappingMode.DEFAULT -> TypeMappingMode.DEFAULT
            KtTypeMappingMode.DEFAULT_UAST -> TypeMappingMode.DEFAULT_UAST
            KtTypeMappingMode.GENERIC_ARGUMENT -> TypeMappingMode.GENERIC_ARGUMENT
            KtTypeMappingMode.SUPER_TYPE -> TypeMappingMode.SUPER_TYPE
            KtTypeMappingMode.SUPER_TYPE_KOTLIN_COLLECTIONS_AS_IS -> TypeMappingMode.SUPER_TYPE_KOTLIN_COLLECTIONS_AS_IS
            KtTypeMappingMode.RETURN_TYPE ->
                rootModuleSession.jvmTypeMapper.typeContext.getOptimalModeForReturnType(type.coneType, isAnnotationMethod)
            KtTypeMappingMode.VALUE_PARAMETER ->
                rootModuleSession.jvmTypeMapper.typeContext.getOptimalModeForValueParameter(type.coneType)
        }
    }
}

private fun ConeKotlinType.simplifyType(
    session: FirSession,
    useSitePosition: PsiElement,
): ConeKotlinType {
    val substitutor = AnonymousTypesSubstitutor(session)
    val visibilityForApproximation = useSitePosition.visibilityForApproximation
    // TODO: See if the given [useSitePosition] is an `inline` method
    val isInlineFunction = false
    var currentType = this
    do {
        val oldType = currentType
        currentType = currentType.fullyExpandedType(session)
        currentType = currentType.upperBoundIfFlexible()
        currentType = substitutor.substituteOrSelf(currentType)
        val needLocalTypeApproximation = needLocalTypeApproximation(visibilityForApproximation, isInlineFunction, session, useSitePosition)
        // TODO: can we approximate local types in type arguments *selectively* ?
        currentType = PublicTypeApproximator.approximateTypeToPublicDenotable(currentType, session, needLocalTypeApproximation)
            ?: currentType

    } while (oldType !== currentType)
    return currentType
}

private fun ConeKotlinType.needLocalTypeApproximation(
    visibilityForApproximation: Visibility,
    isInlineFunction: Boolean,
    session: FirSession,
    useSitePosition: PsiElement
): Boolean {
    if (!shouldHideLocalType(visibilityForApproximation, isInlineFunction)) return false
    val localTypes: List<ConeKotlinType> = if (isLocal(session)) listOf(this) else {
        typeArguments.mapNotNull {
            if (it is ConeKotlinTypeProjection && it.type.isLocal(session)) {
                it.type
            } else null
        }
    }
    val unavailableLocalTypes = localTypes.filterNot { it.isLocalButAvailableAtPosition(session, useSitePosition) }
    // Need to approximate if there are local types that are not available in this scope
    return localTypes.isNotEmpty() && unavailableLocalTypes.isNotEmpty()
}

// Mimic FirDeclaration.visibilityForApproximation
private val PsiElement.visibilityForApproximation: Visibility
    get() {
        if (this !is PsiMember) return Visibilities.Local
        val containerVisibility =
            if (parent is KtLightClassForFacade) Visibilities.Public
            else (parent as? PsiClass)?.visibility ?: Visibilities.Local
        if (containerVisibility == Visibilities.Local || visibility == Visibilities.Local) return Visibilities.Local
        if (containerVisibility == Visibilities.Private) return Visibilities.Private
        return visibility
    }

// Mimic JavaElementUtil#getVisibility
private val PsiModifierListOwner.visibility: Visibility
    get() {
        if (hasModifierProperty(PsiModifier.PUBLIC)) {
            return Visibilities.Public
        }
        if (hasModifierProperty(PsiModifier.PRIVATE) || hasModifierProperty(PsiModifier.PACKAGE_LOCAL)) {
            return Visibilities.Private
        }
        return if (language == JavaLanguage.INSTANCE) {
            when {
                hasModifierProperty(PsiModifier.PROTECTED) && hasModifierProperty(PsiModifier.STATIC) ->
                    JavaVisibilities.ProtectedStaticVisibility
                hasModifierProperty(PsiModifier.PROTECTED) ->
                    JavaVisibilities.ProtectedAndPackage
                else ->
                    JavaVisibilities.PackageVisibility
            }
        } else Visibilities.DEFAULT_VISIBILITY
    }

private fun ConeKotlinType.isLocal(session: FirSession): Boolean {
    return with(session.typeContext) {
        this@isLocal.typeConstructor().isLocalType()
    }
}

private fun ConeKotlinType.isLocalButAvailableAtPosition(
    session: FirSession,
    useSitePosition: PsiElement,
): Boolean {
    val localClassSymbol = this.toRegularClassSymbol(session) ?: return false
    val localPsi = localClassSymbol.source?.psi ?: return false
    val context = (useSitePosition as? KtLightElement<*, *>)?.kotlinOrigin ?: useSitePosition
    // Local type is available if it's inside the same context (containing declaration)
    // or containing declaration is inside the local type, e.g., a member of the local class
    return localPsi == context ||
            localPsi.parents.any { it == context } ||
            context.parents.any { it == localPsi }
}

internal fun ConeKotlinType.asPsiType(
    session: FirSession,
    mode: TypeMappingMode,
    useSitePosition: PsiElement,
): PsiType? {
    val correctedType = simplifyType(session, useSitePosition)

    if (correctedType is ConeErrorType || correctedType !is SimpleTypeMarker) return null

    if (correctedType.typeArguments.any { it is ConeErrorType }) return null

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

    val typeElement = ClsTypeElementImpl(useSitePosition, typeText, '\u0000')
    return typeElement.type
}

private class AnonymousTypesSubstitutor(
    private val session: FirSession,
) : AbstractConeSubstitutor(session.typeContext) {
    override fun substituteType(type: ConeKotlinType): ConeKotlinType? {
        if (type !is ConeClassLikeType) return null

        val isAnonymous = type.classId.let { it?.shortClassName?.asString() == SpecialNames.ANONYMOUS_STRING }
        if (!isAnonymous) return null

        fun ConeClassLikeType.isNotInterface(): Boolean {
            val firClassNode = lookupTag.toSymbol(session) as? FirClassSymbol<*> ?: return false
            return firClassNode.classKind != ClassKind.INTERFACE
        }

        val firClassNode = type.lookupTag.toSymbol(session) as? FirClassSymbol
        if (firClassNode != null) {
            val superTypesCones = firClassNode.resolvedSuperTypes
            val superClass = superTypesCones.firstOrNull { (it as? ConeClassLikeType)?.isNotInterface() == true }
            if (superClass != null) return superClass
        }

        return if (type.nullability.isNullable) session.builtinTypes.nullableAnyType.type
        else session.builtinTypes.anyType.type
    }
}
