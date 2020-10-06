class Candidate(val symbol: AbstractFirBasedSymbol<*>)

abstract class AbstractFirBasedSymbol<E> where E : FirSymbolOwner<E>, E : FirDeclaration {
    lateinit var fir: E
}

interface FirDeclaration

interface FirSymbolOwner<E> where E : FirSymbolOwner<E>, E : FirDeclaration {
    val symbol: AbstractFirBasedSymbol<E>
}

interface FirCallableMemberDeclaration<F : FirCallableMemberDeclaration<F>> : FirSymbolOwner<F>, FirDeclaration {
    override val symbol: AbstractFirBasedSymbol<F>
}

fun foo(candidate: Candidate) {
    val me = candidate.symbol.fir
    if (me is FirCallableMemberDeclaration<*> && me.symbol != null) {}
}