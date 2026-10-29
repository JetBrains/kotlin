/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KaBaseEmptyAnnotationList
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolLocation
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.resolveToFirSymbol
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.fir.analysis.checkers.typeParameterSymbols
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.KtTypeConstraint
import org.jetbrains.kotlin.psi.KtTypeParameterListOwner
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

/**
 * A type parameter for a `where` constraint on a name that is not a declared type parameter,
 * see [FirDeclarationOrigin.Synthetic.DanglingTypeParameter].
 *
 * [backingPsi] is the first constraint subject with this name, so the symbol is created from PSI as declared type parameters are,
 * see [danglingTypeConstraintSubjects].
 * The subject is not a declaration and cannot be passed to the `symbol` API, so [realPsi] is `null`, and the pointer is restored through
 * the containing declaration.
 */
internal class KaFirDanglingTypeParameterSymbol private constructor(
    override val backingPsi: KtSimpleNameExpression?,
    override val analysisSession: KaFirSession,
    override val lazyFirSymbol: Lazy<FirTypeParameterSymbol>,
) : KaFirTypeParameterSymbolBase<KtSimpleNameExpression>() {
    constructor(subject: KtSimpleNameExpression, session: KaFirSession) : this(
        backingPsi = subject,
        lazyFirSymbol = lazyPub { subject.resolveToDanglingTypeParameterSymbol(session) },
        analysisSession = session,
    )

    constructor(symbol: FirTypeParameterSymbol, session: KaFirSession) : this(
        backingPsi = symbol.source.psi as? KtSimpleNameExpression,
        lazyFirSymbol = lazyOf(symbol),
        analysisSession = session,
    )

    override val origin: KaSymbolOrigin
        get() = withValidityAssertion { KaSymbolOrigin.SOURCE_MEMBER_GENERATED }

    override val realPsi: PsiElement?
        get() = withValidityAssertion { null }

    override val anchorPsi: PsiElement?
        get() = withValidityAssertion { backingPsi ?: firSymbol.source.psi }

    override val name: Name
        get() = withValidityAssertion { backingPsi?.getReferencedNameAsName() ?: firSymbol.name }

    /**
     * There is no declaration, so there are no annotations. Annotations in `where` clauses are not annotations of the type parameter:
     * FIR keeps them apart, in [constraintAnnotations][org.jetbrains.kotlin.fir.declarations.FirTypeParameter.constraintAnnotations].
     */
    override val annotations: KaAnnotationList
        get() = withValidityAssertion { KaBaseEmptyAnnotationList(token) }

    override val variance: Variance
        get() = withValidityAssertion { Variance.INVARIANT }

    override val isReified: Boolean
        get() = withValidityAssertion { false }

    override val location: KaSymbolLocation
        get() = withValidityAssertion {
            val isClassTypeParameter = if (backingPsi != null) {
                backingPsi.typeParameterListOwner() is KtClassOrObject
            } else {
                firSymbol.containingDeclarationSymbol is FirClassSymbol<*>
            }

            if (isClassTypeParameter) KaSymbolLocation.CLASS else KaSymbolLocation.LOCAL
        }
}

/**
 * The first subject of the `where` constraints for each name that is not a declared type parameter.
 * The raw FIR builder creates a type parameter for each of them, in the same order, after the declared ones.
 *
 * @see FirDeclarationOrigin.Synthetic.DanglingTypeParameter
 */
internal fun KtTypeParameterListOwner.danglingTypeConstraintSubjects(): List<KtSimpleNameExpression> {
    val typeConstraints = typeConstraints
    if (typeConstraints.isEmpty()) return emptyList()

    val typeParameterNames = typeParameters.mapNotNullTo(hashSetOf()) { it.nameAsName }
    return typeConstraints
        .mapNotNull { it.subjectTypeParameterName }
        .filter { it.getReferencedNameAsName() !in typeParameterNames }
        .distinctBy { it.getReferencedNameAsName() }
}

private fun KtSimpleNameExpression.typeParameterListOwner(): KtTypeParameterListOwner? =
    getStrictParentOfType<KtTypeConstraint>()?.getStrictParentOfType<KtTypeParameterListOwner>()

private fun KtSimpleNameExpression.resolveToDanglingTypeParameterSymbol(session: KaFirSession): FirTypeParameterSymbol {
    val owner = typeParameterListOwner()
        ?: errorWithAttachment("Constraint subject without a type parameter list owner") {
            withPsiEntry("subject", this@resolveToDanglingTypeParameterSymbol)
        }

    val name = getReferencedNameAsName()
    return owner.resolveToFirSymbol(session.resolutionFacade).typeParameterSymbols?.firstOrNull {
        it.name == name && it.origin == FirDeclarationOrigin.Synthetic.DanglingTypeParameter
    } ?: errorWithAttachment("No type parameter for an undeclared constraint subject") {
        withPsiEntry("subject", this@resolveToDanglingTypeParameterSymbol)
        withPsiEntry("owner", owner)
    }
}
