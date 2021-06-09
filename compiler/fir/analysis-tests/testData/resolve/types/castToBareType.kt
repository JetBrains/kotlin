interface FirDeclaration

interface FirSymbolOwner<E : FirSymbolOwner<E>> {
    val symbol: AbstractFirBasedSymbol<<!UPPER_BOUND_VIOLATED!>E<!>>
}
interface FirFunction<F : FirFunction<F>> : FirSymbolOwner<F>, FirDeclaration

interface AbstractFirBasedSymbol<E> where E : FirSymbolOwner<E>, E : FirDeclaration {
    val fir: E
}

fun foo(firAdaptee: FirFunction<*>) {}

fun test(symbol: AbstractFirBasedSymbol<*>) {
    val firAdaptee = symbol.fir as FirFunction
    foo(firAdaptee)
}
