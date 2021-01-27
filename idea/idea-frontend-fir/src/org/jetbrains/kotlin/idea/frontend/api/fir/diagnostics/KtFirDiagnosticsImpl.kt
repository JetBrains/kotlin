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
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtPropertyDelegate
import org.jetbrains.kotlin.psi.KtTypeReference

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

internal class SyntaxImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.Syntax(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class OtherErrorImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.OtherError(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class IllegalConstExpressionImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.IllegalConstExpression(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class IllegalUnderscoreImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.IllegalUnderscore(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class ExpressionRequiredImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExpressionRequired(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class BreakOrContinueOutsideALoopImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.BreakOrContinueOutsideALoop(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class NotALoopLabelImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.NotALoopLabel(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class VariableExpectedImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.VariableExpected(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class ReturnNotAllowedImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ReturnNotAllowed(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class DelegationInInterfaceImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.DelegationInInterface(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class HiddenImpl(
    override val hidden: KtSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.Hidden(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class UnresolvedReferenceImpl(
    override val reference: String,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.UnresolvedReference(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class UnresolvedLabelImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.UnresolvedLabel(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class DeserializationErrorImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.DeserializationError(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class ErrorFromJavaResolutionImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ErrorFromJavaResolution(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class UnknownCallableKindImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.UnknownCallableKind(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class MissingStdlibClassImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.MissingStdlibClass(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class NoThisImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.NoThis(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class SuperIsNotAnExpressionImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.SuperIsNotAnExpression(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class SuperNotAvailableImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.SuperNotAvailable(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class AbstractSuperCallImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.AbstractSuperCall(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class InstanceAccessBeforeSuperCallImpl(
    override val target: String,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.InstanceAccessBeforeSuperCall(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class TypeParameterAsSupertypeImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.TypeParameterAsSupertype(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class EnumAsSupertypeImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.EnumAsSupertype(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class RecursionInSupertypesImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.RecursionInSupertypes(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class NotASupertypeImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.NotASupertype(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class SuperclassNotAccessibleFromInterfaceImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.SuperclassNotAccessibleFromInterface(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class QualifiedSupertypeExtendedByOtherSupertypeImpl(
    override val otherSuperType: KtClassLikeSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.QualifiedSupertypeExtendedByOtherSupertype(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class SupertypeInitializedInInterfaceImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.SupertypeInitializedInInterface(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class InterfaceWithSuperclassImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.InterfaceWithSuperclass(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class ClassInSupertypeForEnumImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ClassInSupertypeForEnum(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class SealedSupertypeImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.SealedSupertype(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class SealedSupertypeInLocalClassImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.SealedSupertypeInLocalClass(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class ConstructorInObjectImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ConstructorInObject(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: KtDeclaration get() = super.psi as KtDeclaration
}

internal class ConstructorInInterfaceImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ConstructorInInterface(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: KtDeclaration get() = super.psi as KtDeclaration
}

internal class NonPrivateConstructorInEnumImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.NonPrivateConstructorInEnum(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class NonPrivateConstructorInSealedImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.NonPrivateConstructorInSealed(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class CyclicConstructorDelegationCallImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.CyclicConstructorDelegationCall(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class PrimaryConstructorDelegationCallExpectedImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.PrimaryConstructorDelegationCallExpected(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class SupertypeInitializedWithoutPrimaryConstructorImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.SupertypeInitializedWithoutPrimaryConstructor(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class DelegationSuperCallInEnumConstructorImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.DelegationSuperCallInEnumConstructor(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class PrimaryConstructorRequiredForDataClassImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.PrimaryConstructorRequiredForDataClass(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class ExplicitDelegationCallRequiredImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExplicitDelegationCallRequired(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class SealedClassConstructorCallImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.SealedClassConstructorCall(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class AnnotationArgumentKclassLiteralOfTypeParameterErrorImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.AnnotationArgumentKclassLiteralOfTypeParameterError(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: KtExpression get() = super.psi as KtExpression
}

internal class AnnotationArgumentMustBeConstImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.AnnotationArgumentMustBeConst(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: KtExpression get() = super.psi as KtExpression
}

internal class AnnotationArgumentMustBeEnumConstImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.AnnotationArgumentMustBeEnumConst(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: KtExpression get() = super.psi as KtExpression
}

internal class AnnotationArgumentMustBeKclassLiteralImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.AnnotationArgumentMustBeKclassLiteral(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: KtExpression get() = super.psi as KtExpression
}

internal class AnnotationClassMemberImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.AnnotationClassMember(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class AnnotationParameterDefaultValueMustBeConstantImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.AnnotationParameterDefaultValueMustBeConstant(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: KtExpression get() = super.psi as KtExpression
}

internal class InvalidTypeOfAnnotationMemberImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.InvalidTypeOfAnnotationMember(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: KtTypeReference get() = super.psi as KtTypeReference
}

internal class LocalAnnotationClassErrorImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.LocalAnnotationClassError(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: KtClassOrObject get() = super.psi as KtClassOrObject
}

internal class MissingValOnAnnotationParameterImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.MissingValOnAnnotationParameter(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: KtParameter get() = super.psi as KtParameter
}

internal class NonConstValUsedInConstantExpressionImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.NonConstValUsedInConstantExpression(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: KtExpression get() = super.psi as KtExpression
}

internal class NotAnAnnotationClassImpl(
    override val annotationName: String,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.NotAnAnnotationClass(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class NullableTypeOfAnnotationMemberImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.NullableTypeOfAnnotationMember(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: KtTypeReference get() = super.psi as KtTypeReference
}

internal class VarAnnotationParameterImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.VarAnnotationParameter(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: KtParameter get() = super.psi as KtParameter
}

internal class ExposedTypealiasExpandedTypeImpl(
    override val elementVisibility: Visibility,
    override val restrictingDeclaration: KtSymbol,
    override val restrictingVisibility: Visibility,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExposedTypealiasExpandedType(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: KtNamedDeclaration get() = super.psi as KtNamedDeclaration
}

internal class ExposedFunctionReturnTypeImpl(
    override val elementVisibility: Visibility,
    override val restrictingDeclaration: KtSymbol,
    override val restrictingVisibility: Visibility,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExposedFunctionReturnType(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: KtNamedDeclaration get() = super.psi as KtNamedDeclaration
}

internal class ExposedReceiverTypeImpl(
    override val elementVisibility: Visibility,
    override val restrictingDeclaration: KtSymbol,
    override val restrictingVisibility: Visibility,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExposedReceiverType(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: KtTypeReference get() = super.psi as KtTypeReference
}

internal class ExposedPropertyTypeImpl(
    override val elementVisibility: Visibility,
    override val restrictingDeclaration: KtSymbol,
    override val restrictingVisibility: Visibility,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExposedPropertyType(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: KtNamedDeclaration get() = super.psi as KtNamedDeclaration
}

internal class ExposedParameterTypeImpl(
    override val elementVisibility: Visibility,
    override val restrictingDeclaration: KtSymbol,
    override val restrictingVisibility: Visibility,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExposedParameterType(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: KtParameter get() = super.psi as KtParameter
}

internal class ExposedSuperInterfaceImpl(
    override val elementVisibility: Visibility,
    override val restrictingDeclaration: KtSymbol,
    override val restrictingVisibility: Visibility,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExposedSuperInterface(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: KtTypeReference get() = super.psi as KtTypeReference
}

internal class ExposedSuperClassImpl(
    override val elementVisibility: Visibility,
    override val restrictingDeclaration: KtSymbol,
    override val restrictingVisibility: Visibility,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExposedSuperClass(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: KtTypeReference get() = super.psi as KtTypeReference
}

internal class ExposedTypeParameterBoundImpl(
    override val elementVisibility: Visibility,
    override val restrictingDeclaration: KtSymbol,
    override val restrictingVisibility: Visibility,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExposedTypeParameterBound(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: KtTypeReference get() = super.psi as KtTypeReference
}

internal class InapplicableInfixModifierImpl(
    override val modifier: String,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.InapplicableInfixModifier(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class RepeatedModifierImpl(
    override val modifier: KtModifierKeywordToken,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.RepeatedModifier(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class RedundantModifierImpl(
    override val redundantModifier: KtModifierKeywordToken,
    override val conflictingModifier: KtModifierKeywordToken,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.RedundantModifier(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class DeprecatedModifierPairImpl(
    override val deprecatedModifier: KtModifierKeywordToken,
    override val conflictingModifier: KtModifierKeywordToken,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.DeprecatedModifierPair(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class IncompatibleModifiersImpl(
    override val modifier1: KtModifierKeywordToken,
    override val modifier2: KtModifierKeywordToken,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.IncompatibleModifiers(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class RedundantOpenInInterfaceImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.RedundantOpenInInterface(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: KtModifierListOwner get() = super.psi as KtModifierListOwner
}

internal class NoneApplicableImpl(
    override val candidates: List<KtSymbol>,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.NoneApplicable(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class InapplicableCandidateImpl(
    override val candidate: KtSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.InapplicableCandidate(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class InapplicableLateinitModifierImpl(
    override val reason: String,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.InapplicableLateinitModifier(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class AmbiguityImpl(
    override val candidates: List<KtSymbol>,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.Ambiguity(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class AssignOperatorAmbiguityImpl(
    override val candidates: List<KtSymbol>,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.AssignOperatorAmbiguity(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class TypeMismatchImpl(
    override val expectedType: KtType,
    override val actualType: KtType,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.TypeMismatch(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class RecursionInImplicitTypesImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.RecursionInImplicitTypes(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class InferenceErrorImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.InferenceError(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class ProjectionOnNonClassTypeArgumentImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ProjectionOnNonClassTypeArgument(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class UpperBoundViolatedImpl(
    override val typeParameter: KtTypeParameterSymbol,
    override val violatedType: KtType,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.UpperBoundViolated(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class TypeArgumentsNotAllowedImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.TypeArgumentsNotAllowed(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class WrongNumberOfTypeArgumentsImpl(
    override val expectedCount: Int,
    override val classifier: KtClassLikeSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.WrongNumberOfTypeArguments(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class NoTypeForTypeParameterImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.NoTypeForTypeParameter(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class TypeParametersInObjectImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.TypeParametersInObject(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class IllegalProjectionUsageImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.IllegalProjectionUsage(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class TypeParametersInEnumImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.TypeParametersInEnum(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class ConflictingProjectionImpl(
    override val type: String,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ConflictingProjection(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class VarianceOnTypeParameterNotAllowedImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.VarianceOnTypeParameterNotAllowed(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class ReturnTypeMismatchOnOverrideImpl(
    override val returnType: String,
    override val superFunction: KtSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ReturnTypeMismatchOnOverride(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class PropertyTypeMismatchOnOverrideImpl(
    override val propertyType: String,
    override val targetProperty: KtSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.PropertyTypeMismatchOnOverride(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class VarTypeMismatchOnOverrideImpl(
    override val variableType: String,
    override val targetVariable: KtSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.VarTypeMismatchOnOverride(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class ManyCompanionObjectsImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ManyCompanionObjects(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class ConflictingOverloadsImpl(
    override val conflictingOverloads: String,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ConflictingOverloads(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class RedeclarationImpl(
    override val conflictingDeclaration: String,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.Redeclaration(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class AnyMethodImplementedInInterfaceImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.AnyMethodImplementedInInterface(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class LocalObjectNotAllowedImpl(
    override val objectName: Name,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.LocalObjectNotAllowed(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: KtNamedDeclaration get() = super.psi as KtNamedDeclaration
}

internal class LocalInterfaceNotAllowedImpl(
    override val interfaceName: Name,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.LocalInterfaceNotAllowed(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: KtNamedDeclaration get() = super.psi as KtNamedDeclaration
}

internal class AbstractFunctionInNonAbstractClassImpl(
    override val function: KtSymbol,
    override val containingClass: KtSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.AbstractFunctionInNonAbstractClass(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: KtFunction get() = super.psi as KtFunction
}

internal class AbstractFunctionWithBodyImpl(
    override val function: KtSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.AbstractFunctionWithBody(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: KtFunction get() = super.psi as KtFunction
}

internal class NonAbstractFunctionWithNoBodyImpl(
    override val function: KtSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.NonAbstractFunctionWithNoBody(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: KtFunction get() = super.psi as KtFunction
}

internal class PrivateFunctionWithNoBodyImpl(
    override val function: KtSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.PrivateFunctionWithNoBody(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: KtFunction get() = super.psi as KtFunction
}

internal class NonMemberFunctionNoBodyImpl(
    override val function: KtSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.NonMemberFunctionNoBody(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: KtFunction get() = super.psi as KtFunction
}

internal class FunctionDeclarationWithNoNameImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.FunctionDeclarationWithNoName(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: KtFunction get() = super.psi as KtFunction
}

internal class AnonymousFunctionParameterWithDefaultValueImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.AnonymousFunctionParameterWithDefaultValue(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: KtParameter get() = super.psi as KtParameter
}

internal class UselessVarargOnParameterImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.UselessVarargOnParameter(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: KtParameter get() = super.psi as KtParameter
}

internal class AbstractPropertyInNonAbstractClassImpl(
    override val property: KtSymbol,
    override val containingClass: KtSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.AbstractPropertyInNonAbstractClass(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: KtModifierListOwner get() = super.psi as KtModifierListOwner
}

internal class PrivatePropertyInInterfaceImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.PrivatePropertyInInterface(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: KtProperty get() = super.psi as KtProperty
}

internal class AbstractPropertyWithInitializerImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.AbstractPropertyWithInitializer(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: KtExpression get() = super.psi as KtExpression
}

internal class PropertyInitializerInInterfaceImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.PropertyInitializerInInterface(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: KtExpression get() = super.psi as KtExpression
}

internal class PropertyWithNoTypeNoInitializerImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.PropertyWithNoTypeNoInitializer(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: KtProperty get() = super.psi as KtProperty
}

internal class AbstractDelegatedPropertyImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.AbstractDelegatedProperty(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: KtPropertyDelegate get() = super.psi as KtPropertyDelegate
}

internal class DelegatedPropertyInInterfaceImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.DelegatedPropertyInInterface(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: KtPropertyDelegate get() = super.psi as KtPropertyDelegate
}

internal class AbstractPropertyWithGetterImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.AbstractPropertyWithGetter(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: KtPropertyAccessor get() = super.psi as KtPropertyAccessor
}

internal class AbstractPropertyWithSetterImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.AbstractPropertyWithSetter(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: KtPropertyAccessor get() = super.psi as KtPropertyAccessor
}

internal class PrivateSetterForAbstractPropertyImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.PrivateSetterForAbstractProperty(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class PrivateSetterForOpenPropertyImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.PrivateSetterForOpenProperty(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class ExpectedPrivateDeclarationImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExpectedPrivateDeclaration(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: KtModifierListOwner get() = super.psi as KtModifierListOwner
}

internal class ExpectedDeclarationWithBodyImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExpectedDeclarationWithBody(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: KtDeclaration get() = super.psi as KtDeclaration
}

internal class ExpectedPropertyInitializerImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExpectedPropertyInitializer(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: KtExpression get() = super.psi as KtExpression
}

internal class ExpectedDelegatedPropertyImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ExpectedDelegatedProperty(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: KtPropertyDelegate get() = super.psi as KtPropertyDelegate
}

internal class InitializerRequiredForDestructuringDeclarationImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.InitializerRequiredForDestructuringDeclaration(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: KtDestructuringDeclaration get() = super.psi as KtDestructuringDeclaration
}

internal class ComponentFunctionMissingImpl(
    override val missingFunctionName: Name,
    override val destructingType: KtType,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ComponentFunctionMissing(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class ComponentFunctionAmbiguityImpl(
    override val functionWithAmbiguityName: Name,
    override val candidates: List<KtSymbol>,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ComponentFunctionAmbiguity(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class UninitializedVariableImpl(
    override val variable: KtVariableSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.UninitializedVariable(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class WrongInvocationKindImpl(
    override val declaration: KtSymbol,
    override val requiredRange: EventOccurrencesRange,
    override val actualRange: EventOccurrencesRange,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.WrongInvocationKind(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class LeakedInPlaceLambdaImpl(
    override val lambda: KtSymbol,
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.LeakedInPlaceLambda(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class WrongImpliesConditionImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.WrongImpliesCondition(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class RedundantVisibilityModifierImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.RedundantVisibilityModifier(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: KtModifierListOwner get() = super.psi as KtModifierListOwner
}

internal class RedundantModalityModifierImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.RedundantModalityModifier(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: KtModifierListOwner get() = super.psi as KtModifierListOwner
}

internal class RedundantReturnUnitTypeImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.RedundantReturnUnitType(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiTypeElement get() = super.psi as PsiTypeElement
}

internal class RedundantExplicitTypeImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.RedundantExplicitType(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class RedundantSingleExpressionStringTemplateImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.RedundantSingleExpressionStringTemplate(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class CanBeValImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.CanBeVal(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: KtDeclaration get() = super.psi as KtDeclaration
}

internal class CanBeReplacedWithOperatorAssignmentImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.CanBeReplacedWithOperatorAssignment(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: KtExpression get() = super.psi as KtExpression
}

internal class RedundantCallOfConversionMethodImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.RedundantCallOfConversionMethod(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class ArrayEqualityOperatorCanBeReplacedWithEqualsImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.ArrayEqualityOperatorCanBeReplacedWithEquals(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: KtExpression get() = super.psi as KtExpression
}

internal class EmptyRangeImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.EmptyRange(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class RedundantSetterParameterTypeImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.RedundantSetterParameterType(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class UnusedVariableImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.UnusedVariable(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: KtNamedDeclaration get() = super.psi as KtNamedDeclaration
}

internal class AssignedValueIsNeverReadImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.AssignedValueIsNeverRead(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class VariableInitializerIsRedundantImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.VariableInitializerIsRedundant(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

internal class VariableNeverReadImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.VariableNeverRead(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: KtNamedDeclaration get() = super.psi as KtNamedDeclaration
}

internal class UselessCallOnNotNullImpl(
    firDiagnostic: FirPsiDiagnostic<*>,
    override val token: ValidityToken,
) : KtFirDiagnostic.UselessCallOnNotNull(), KtAbstractFirDiagnostic {
    override val firDiagnostic: FirPsiDiagnostic<*> by weakRef(firDiagnostic)
    override val psi: PsiElement get() = super.psi
}

