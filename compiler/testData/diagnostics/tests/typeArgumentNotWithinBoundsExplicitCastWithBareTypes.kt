// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-78304
// DIAGNOSTICS: -UNCHECKED_CAST
// LANGUAGE: +ForbidUpperBoundsViolationOnTypeOperatorAndParameterBounds

open class FirDeclaration
open class FirClassLikeDeclaration : FirDeclaration()

open class FirBasedSymbol<out E : FirDeclaration>
open class FirClassLikeSymbol<out D : FirClassLikeDeclaration> : FirBasedSymbol<D>()

fun main() {
    FirBasedSymbol<FirDeclaration>() is FirClassLikeSymbol

    when (FirBasedSymbol<FirDeclaration>()) {
        is FirClassLikeSymbol -> {}
        else -> {}
    }

    FirBasedSymbol<FirDeclaration>() as FirClassLikeSymbol
}

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, functionDeclaration, out, typeConstraint, typeParameter */
