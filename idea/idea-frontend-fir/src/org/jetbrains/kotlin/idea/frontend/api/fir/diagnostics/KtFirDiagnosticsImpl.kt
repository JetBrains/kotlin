/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.diagnostics

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiTypeElement
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirPsiDiagnostic
import org.jetbrains.kotlin.idea.frontend.api.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.weakRef
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtClassLikeSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtTypeParameterSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtVariableSymbol
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtPropertyDelegate
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.psi.KtTypeParameterList
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.KtWhenExpression

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class SyntaxImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.Syntax(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class OtherErrorImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.OtherError(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class IllegalConstExpressionImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.IllegalConstExpression(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class IllegalUnderscoreImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.IllegalUnderscore(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ExpressionRequiredImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExpressionRequired(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class BreakOrContinueOutsideALoopImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.BreakOrContinueOutsideALoop(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class NotALoopLabelImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.NotALoopLabel(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class VariableExpectedImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.VariableExpected(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ReturnNotAllowedImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ReturnNotAllowed(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class DelegationInInterfaceImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.DelegationInInterface(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class HiddenImpl(
    override val hidden: KtSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.Hidden(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class UnresolvedReferenceImpl(
    override val reference: String,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.UnresolvedReference(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class UnresolvedLabelImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.UnresolvedLabel(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class DeserializationErrorImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.DeserializationError(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ErrorFromJavaResolutionImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ErrorFromJavaResolution(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class UnknownCallableKindImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.UnknownCallableKind(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class MissingStdlibClassImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.MissingStdlibClass(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class NoThisImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.NoThis(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class SuperIsNotAnExpressionImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.SuperIsNotAnExpression(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class SuperNotAvailableImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.SuperNotAvailable(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class AbstractSuperCallImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.AbstractSuperCall(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class InstanceAccessBeforeSuperCallImpl(
    override val target: String,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.InstanceAccessBeforeSuperCall(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class TypeParameterAsSupertypeImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.TypeParameterAsSupertype(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class EnumAsSupertypeImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.EnumAsSupertype(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class RecursionInSupertypesImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.RecursionInSupertypes(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class NotASupertypeImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.NotASupertype(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class SuperclassNotAccessibleFromInterfaceImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.SuperclassNotAccessibleFromInterface(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class QualifiedSupertypeExtendedByOtherSupertypeImpl(
    override val otherSuperType: KtClassLikeSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.QualifiedSupertypeExtendedByOtherSupertype(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class SupertypeInitializedInInterfaceImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.SupertypeInitializedInInterface(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class InterfaceWithSuperclassImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.InterfaceWithSuperclass(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ClassInSupertypeForEnumImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ClassInSupertypeForEnum(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class SealedSupertypeImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.SealedSupertype(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class SealedSupertypeInLocalClassImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.SealedSupertypeInLocalClass(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ConstructorInObjectImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ConstructorInObject(), KtAbstractFirDiagnostic<KtDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ConstructorInInterfaceImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ConstructorInInterface(), KtAbstractFirDiagnostic<KtDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class NonPrivateConstructorInEnumImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.NonPrivateConstructorInEnum(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class NonPrivateConstructorInSealedImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.NonPrivateConstructorInSealed(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class CyclicConstructorDelegationCallImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.CyclicConstructorDelegationCall(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class PrimaryConstructorDelegationCallExpectedImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.PrimaryConstructorDelegationCallExpected(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class SupertypeInitializedWithoutPrimaryConstructorImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.SupertypeInitializedWithoutPrimaryConstructor(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class DelegationSuperCallInEnumConstructorImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.DelegationSuperCallInEnumConstructor(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class PrimaryConstructorRequiredForDataClassImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.PrimaryConstructorRequiredForDataClass(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ExplicitDelegationCallRequiredImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExplicitDelegationCallRequired(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class SealedClassConstructorCallImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.SealedClassConstructorCall(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class AnnotationArgumentKclassLiteralOfTypeParameterErrorImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.AnnotationArgumentKclassLiteralOfTypeParameterError(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class AnnotationArgumentMustBeConstImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.AnnotationArgumentMustBeConst(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class AnnotationArgumentMustBeEnumConstImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.AnnotationArgumentMustBeEnumConst(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class AnnotationArgumentMustBeKclassLiteralImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.AnnotationArgumentMustBeKclassLiteral(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class AnnotationClassMemberImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.AnnotationClassMember(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class AnnotationParameterDefaultValueMustBeConstantImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.AnnotationParameterDefaultValueMustBeConstant(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class InvalidTypeOfAnnotationMemberImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.InvalidTypeOfAnnotationMember(), KtAbstractFirDiagnostic<KtTypeReference> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class LocalAnnotationClassErrorImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.LocalAnnotationClassError(), KtAbstractFirDiagnostic<KtClassOrObject> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class MissingValOnAnnotationParameterImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.MissingValOnAnnotationParameter(), KtAbstractFirDiagnostic<KtParameter> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class NonConstValUsedInConstantExpressionImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.NonConstValUsedInConstantExpression(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class NotAnAnnotationClassImpl(
    override val annotationName: String,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.NotAnAnnotationClass(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class NullableTypeOfAnnotationMemberImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.NullableTypeOfAnnotationMember(), KtAbstractFirDiagnostic<KtTypeReference> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class VarAnnotationParameterImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.VarAnnotationParameter(), KtAbstractFirDiagnostic<KtParameter> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ExposedTypealiasExpandedTypeImpl(
    override val elementVisibility: Visibility,
    override val restrictingDeclaration: KtSymbol,
    override val restrictingVisibility: Visibility,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExposedTypealiasExpandedType(), KtAbstractFirDiagnostic<KtNamedDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ExposedFunctionReturnTypeImpl(
    override val elementVisibility: Visibility,
    override val restrictingDeclaration: KtSymbol,
    override val restrictingVisibility: Visibility,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExposedFunctionReturnType(), KtAbstractFirDiagnostic<KtNamedDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ExposedReceiverTypeImpl(
    override val elementVisibility: Visibility,
    override val restrictingDeclaration: KtSymbol,
    override val restrictingVisibility: Visibility,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExposedReceiverType(), KtAbstractFirDiagnostic<KtTypeReference> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ExposedPropertyTypeImpl(
    override val elementVisibility: Visibility,
    override val restrictingDeclaration: KtSymbol,
    override val restrictingVisibility: Visibility,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExposedPropertyType(), KtAbstractFirDiagnostic<KtNamedDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ExposedParameterTypeImpl(
    override val elementVisibility: Visibility,
    override val restrictingDeclaration: KtSymbol,
    override val restrictingVisibility: Visibility,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExposedParameterType(), KtAbstractFirDiagnostic<KtParameter> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ExposedSuperInterfaceImpl(
    override val elementVisibility: Visibility,
    override val restrictingDeclaration: KtSymbol,
    override val restrictingVisibility: Visibility,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExposedSuperInterface(), KtAbstractFirDiagnostic<KtTypeReference> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ExposedSuperClassImpl(
    override val elementVisibility: Visibility,
    override val restrictingDeclaration: KtSymbol,
    override val restrictingVisibility: Visibility,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExposedSuperClass(), KtAbstractFirDiagnostic<KtTypeReference> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ExposedTypeParameterBoundImpl(
    override val elementVisibility: Visibility,
    override val restrictingDeclaration: KtSymbol,
    override val restrictingVisibility: Visibility,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExposedTypeParameterBound(), KtAbstractFirDiagnostic<KtTypeReference> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class InapplicableInfixModifierImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.InapplicableInfixModifier(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class RepeatedModifierImpl(
    override val modifier: KtModifierKeywordToken,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.RepeatedModifier(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class RedundantModifierImpl(
    override val redundantModifier: KtModifierKeywordToken,
    override val conflictingModifier: KtModifierKeywordToken,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.RedundantModifier(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class DeprecatedModifierPairImpl(
    override val deprecatedModifier: KtModifierKeywordToken,
    override val conflictingModifier: KtModifierKeywordToken,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.DeprecatedModifierPair(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class IncompatibleModifiersImpl(
    override val modifier1: KtModifierKeywordToken,
    override val modifier2: KtModifierKeywordToken,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.IncompatibleModifiers(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class RedundantOpenInInterfaceImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.RedundantOpenInInterface(), KtAbstractFirDiagnostic<KtModifierListOwner> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class NoneApplicableImpl(
    override val candidates: List<KtSymbol>,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.NoneApplicable(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class InapplicableCandidateImpl(
    override val candidate: KtSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.InapplicableCandidate(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class InapplicableLateinitModifierImpl(
    override val reason: String,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.InapplicableLateinitModifier(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class AmbiguityImpl(
    override val candidates: List<KtSymbol>,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.Ambiguity(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class AssignOperatorAmbiguityImpl(
    override val candidates: List<KtSymbol>,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.AssignOperatorAmbiguity(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class TypeMismatchImpl(
    override val expectedType: KtType,
    override val actualType: KtType,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.TypeMismatch(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class RecursionInImplicitTypesImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.RecursionInImplicitTypes(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class InferenceErrorImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.InferenceError(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ProjectionOnNonClassTypeArgumentImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ProjectionOnNonClassTypeArgument(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class UpperBoundViolatedImpl(
    override val typeParameter: KtTypeParameterSymbol,
    override val violatedType: KtType,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.UpperBoundViolated(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class TypeArgumentsNotAllowedImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.TypeArgumentsNotAllowed(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class WrongNumberOfTypeArgumentsImpl(
    override val expectedCount: Int,
    override val classifier: KtClassLikeSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.WrongNumberOfTypeArguments(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class NoTypeForTypeParameterImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.NoTypeForTypeParameter(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class TypeParametersInObjectImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.TypeParametersInObject(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class IllegalProjectionUsageImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.IllegalProjectionUsage(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class TypeParametersInEnumImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.TypeParametersInEnum(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ConflictingProjectionImpl(
    override val type: String,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ConflictingProjection(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class VarianceOnTypeParameterNotAllowedImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.VarianceOnTypeParameterNotAllowed(), KtAbstractFirDiagnostic<KtTypeParameter> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class CatchParameterWithDefaultValueImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.CatchParameterWithDefaultValue(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ReifiedTypeInCatchClauseImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ReifiedTypeInCatchClause(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class TypeParameterInCatchClauseImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.TypeParameterInCatchClause(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class GenericThrowableSubclassImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.GenericThrowableSubclass(), KtAbstractFirDiagnostic<KtTypeParameterList> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class InnerClassOfGenericThrowableSubclassImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.InnerClassOfGenericThrowableSubclass(), KtAbstractFirDiagnostic<KtClassOrObject> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class NothingToOverrideImpl(
    override val declaration: KtSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.NothingToOverride(), KtAbstractFirDiagnostic<KtModifierListOwner> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class CannotWeakenAccessPrivilegeImpl(
    override val overridingVisibility: Visibility,
    override val overridden: KtCallableSymbol,
    override val containingClassName: Name,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.CannotWeakenAccessPrivilege(), KtAbstractFirDiagnostic<KtModifierListOwner> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class CannotChangeAccessPrivilegeImpl(
    override val overridingVisibility: Visibility,
    override val overridden: KtCallableSymbol,
    override val containingClassName: Name,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.CannotChangeAccessPrivilege(), KtAbstractFirDiagnostic<KtModifierListOwner> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class OverridingFinalMemberImpl(
    override val overriddenDeclaration: KtCallableSymbol,
    override val containingClassName: Name,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.OverridingFinalMember(), KtAbstractFirDiagnostic<KtNamedDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ReturnTypeMismatchOnOverrideImpl(
    override val function: KtSymbol,
    override val superFunction: KtSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ReturnTypeMismatchOnOverride(), KtAbstractFirDiagnostic<KtNamedDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class PropertyTypeMismatchOnOverrideImpl(
    override val property: KtSymbol,
    override val superProperty: KtSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.PropertyTypeMismatchOnOverride(), KtAbstractFirDiagnostic<KtNamedDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class VarTypeMismatchOnOverrideImpl(
    override val variable: KtSymbol,
    override val superVariable: KtSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.VarTypeMismatchOnOverride(), KtAbstractFirDiagnostic<KtNamedDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class VarOverriddenByValImpl(
    override val overridingDeclaration: KtSymbol,
    override val overriddenDeclaration: KtSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.VarOverriddenByVal(), KtAbstractFirDiagnostic<KtNamedDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ManyCompanionObjectsImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ManyCompanionObjects(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ConflictingOverloadsImpl(
    override val conflictingOverloads: String,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ConflictingOverloads(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class RedeclarationImpl(
    override val conflictingDeclaration: String,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.Redeclaration(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class AnyMethodImplementedInInterfaceImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.AnyMethodImplementedInInterface(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class LocalObjectNotAllowedImpl(
    override val objectName: Name,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.LocalObjectNotAllowed(), KtAbstractFirDiagnostic<KtNamedDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class LocalInterfaceNotAllowedImpl(
    override val interfaceName: Name,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.LocalInterfaceNotAllowed(), KtAbstractFirDiagnostic<KtNamedDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class AbstractFunctionInNonAbstractClassImpl(
    override val function: KtSymbol,
    override val containingClass: KtSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.AbstractFunctionInNonAbstractClass(), KtAbstractFirDiagnostic<KtFunction> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class AbstractFunctionWithBodyImpl(
    override val function: KtSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.AbstractFunctionWithBody(), KtAbstractFirDiagnostic<KtFunction> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class NonAbstractFunctionWithNoBodyImpl(
    override val function: KtSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.NonAbstractFunctionWithNoBody(), KtAbstractFirDiagnostic<KtFunction> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class PrivateFunctionWithNoBodyImpl(
    override val function: KtSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.PrivateFunctionWithNoBody(), KtAbstractFirDiagnostic<KtFunction> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class NonMemberFunctionNoBodyImpl(
    override val function: KtSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.NonMemberFunctionNoBody(), KtAbstractFirDiagnostic<KtFunction> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class FunctionDeclarationWithNoNameImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.FunctionDeclarationWithNoName(), KtAbstractFirDiagnostic<KtFunction> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class AnonymousFunctionParameterWithDefaultValueImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.AnonymousFunctionParameterWithDefaultValue(), KtAbstractFirDiagnostic<KtParameter> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class UselessVarargOnParameterImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.UselessVarargOnParameter(), KtAbstractFirDiagnostic<KtParameter> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class AbstractPropertyInNonAbstractClassImpl(
    override val property: KtSymbol,
    override val containingClass: KtSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.AbstractPropertyInNonAbstractClass(), KtAbstractFirDiagnostic<KtModifierListOwner> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class PrivatePropertyInInterfaceImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.PrivatePropertyInInterface(), KtAbstractFirDiagnostic<KtProperty> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class AbstractPropertyWithInitializerImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.AbstractPropertyWithInitializer(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class PropertyInitializerInInterfaceImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.PropertyInitializerInInterface(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class PropertyWithNoTypeNoInitializerImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.PropertyWithNoTypeNoInitializer(), KtAbstractFirDiagnostic<KtProperty> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class BackingFieldInInterfaceImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.BackingFieldInInterface(), KtAbstractFirDiagnostic<KtProperty> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ExtensionPropertyWithBackingFieldImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExtensionPropertyWithBackingField(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class PropertyInitializerNoBackingFieldImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.PropertyInitializerNoBackingField(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class AbstractDelegatedPropertyImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.AbstractDelegatedProperty(), KtAbstractFirDiagnostic<KtPropertyDelegate> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class DelegatedPropertyInInterfaceImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.DelegatedPropertyInInterface(), KtAbstractFirDiagnostic<KtPropertyDelegate> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class AbstractPropertyWithGetterImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.AbstractPropertyWithGetter(), KtAbstractFirDiagnostic<KtPropertyAccessor> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class AbstractPropertyWithSetterImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.AbstractPropertyWithSetter(), KtAbstractFirDiagnostic<KtPropertyAccessor> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class PrivateSetterForAbstractPropertyImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.PrivateSetterForAbstractProperty(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class PrivateSetterForOpenPropertyImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.PrivateSetterForOpenProperty(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ExpectedPrivateDeclarationImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExpectedPrivateDeclaration(), KtAbstractFirDiagnostic<KtModifierListOwner> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ExpectedDeclarationWithBodyImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExpectedDeclarationWithBody(), KtAbstractFirDiagnostic<KtDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ExpectedPropertyInitializerImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExpectedPropertyInitializer(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ExpectedDelegatedPropertyImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExpectedDelegatedProperty(), KtAbstractFirDiagnostic<KtPropertyDelegate> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class InitializerRequiredForDestructuringDeclarationImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.InitializerRequiredForDestructuringDeclaration(), KtAbstractFirDiagnostic<KtDestructuringDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ComponentFunctionMissingImpl(
    override val missingFunctionName: Name,
    override val destructingType: KtType,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ComponentFunctionMissing(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ComponentFunctionAmbiguityImpl(
    override val functionWithAmbiguityName: Name,
    override val candidates: List<KtSymbol>,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ComponentFunctionAmbiguity(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ComponentFunctionOnNullableImpl(
    override val componentFunctionName: Name,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ComponentFunctionOnNullable(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class UninitializedVariableImpl(
    override val variable: KtVariableSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.UninitializedVariable(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class WrongInvocationKindImpl(
    override val declaration: KtSymbol,
    override val requiredRange: EventOccurrencesRange,
    override val actualRange: EventOccurrencesRange,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.WrongInvocationKind(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class LeakedInPlaceLambdaImpl(
    override val lambda: KtSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.LeakedInPlaceLambda(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class WrongImpliesConditionImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.WrongImpliesCondition(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class UnsafeCallImpl(
    override val receiverType: KtType,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.UnsafeCall(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class UnsafeImplicitInvokeCallImpl(
    override val receiverType: KtType,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.UnsafeImplicitInvokeCall(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class UnsafeInfixCallImpl(
    override val lhs: KtExpression,
    override val operator: String,
    override val rhs: KtExpression,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.UnsafeInfixCall(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class UnsafeOperatorCallImpl(
    override val lhs: KtExpression,
    override val operator: String,
    override val rhs: KtExpression,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.UnsafeOperatorCall(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class NoElseInWhenImpl(
    override val missingWhenCases: List<Any>,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.NoElseInWhen(), KtAbstractFirDiagnostic<KtWhenExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class InvalidIfAsExpressionImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.InvalidIfAsExpression(), KtAbstractFirDiagnostic<KtIfExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class RedundantVisibilityModifierImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.RedundantVisibilityModifier(), KtAbstractFirDiagnostic<KtModifierListOwner> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class RedundantModalityModifierImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.RedundantModalityModifier(), KtAbstractFirDiagnostic<KtModifierListOwner> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class RedundantReturnUnitTypeImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.RedundantReturnUnitType(), KtAbstractFirDiagnostic<PsiTypeElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class RedundantExplicitTypeImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.RedundantExplicitType(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class RedundantSingleExpressionStringTemplateImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.RedundantSingleExpressionStringTemplate(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class CanBeValImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.CanBeVal(), KtAbstractFirDiagnostic<KtDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class CanBeReplacedWithOperatorAssignmentImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.CanBeReplacedWithOperatorAssignment(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class RedundantCallOfConversionMethodImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.RedundantCallOfConversionMethod(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class ArrayEqualityOperatorCanBeReplacedWithEqualsImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ArrayEqualityOperatorCanBeReplacedWithEquals(), KtAbstractFirDiagnostic<KtExpression> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class EmptyRangeImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.EmptyRange(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class RedundantSetterParameterTypeImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.RedundantSetterParameterType(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class UnusedVariableImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.UnusedVariable(), KtAbstractFirDiagnostic<KtNamedDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class AssignedValueIsNeverReadImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.AssignedValueIsNeverRead(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class VariableInitializerIsRedundantImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.VariableInitializerIsRedundant(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class VariableNeverReadImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.VariableNeverRead(), KtAbstractFirDiagnostic<KtNamedDeclaration> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

internal class UselessCallOnNotNullImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.UselessCallOnNotNull(), KtAbstractFirDiagnostic<PsiElement> {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
}

