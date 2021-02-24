interface FirTypeScope

interface AbstractFirBasedSymbol<E> where E : FirSymbolOwner<E>, E : FirDeclaration {
    val fir: E
}

interface FirCallableSymbol<D : FirCallableDeclaration<D>> : AbstractFirBasedSymbol<D>

interface FirElement
interface FirSymbolOwner<E> : FirElement where E : FirSymbolOwner<E>, E : FirDeclaration {
    val symbol:  AbstractFirBasedSymbol<E>
}
interface FirDeclaration : FirElement
interface FirCallableDeclaration<F : FirCallableDeclaration<F>> : FirDeclaration, FirSymbolOwner<F>
interface FirCallableMemberDeclaration<F : FirCallableMemberDeclaration<F>> : FirCallableDeclaration<F>

private inline fun <reified S : FirCallableSymbol<*>> computeBaseSymbols(
    symbol: S,
    basedSymbol: S,
    directOverridden: FirTypeScope.(S) -> List<S>
) {}

fun FirCallableSymbol<*>.dispatchReceiverClassOrNull(): Boolean? = true

private inline fun <reified D : FirCallableMemberDeclaration<D>, reified S : FirCallableSymbol<D>> createFakeOverriddenIfNeeded(
    originalSymbol: FirCallableSymbol<*>,
    basedSymbol: S,
    computeDirectOverridden: FirTypeScope.(S) -> List<S>,
    someCondition: Boolean
) {
    if (originalSymbol !is S) return
    if (originalSymbol.dispatchReceiverClassOrNull() == true && someCondition) return
    computeBaseSymbols(originalSymbol, basedSymbol, computeDirectOverridden)
}
