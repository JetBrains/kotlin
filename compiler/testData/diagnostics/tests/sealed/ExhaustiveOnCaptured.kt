// FIR_IDENTICAL

abstract class FirBasedSymbol<E : FirDeclaration> {
    val fir: E get() = null!!
}
abstract class FirCallableSymbol<D : FirCallableDeclaration> : FirBasedSymbol<D>()

sealed class FirDeclaration
sealed class FirCallableDeclaration : FirDeclaration()

class FirFunction : FirCallableDeclaration()
class FirVariable : FirCallableDeclaration()

val FirCallableSymbol<*>.isExtension: Boolean
    get() = when (fir) {
        is FirFunction -> true
        is FirVariable -> false
    }