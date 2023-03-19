// K1 generates `accept(Object)` and bridge `accept(U)` in the SAM adapter.
// K2 generates `accept(X)` and bridge `accept(U)`. It is fine though, because if `check`'s parameter is passed where Z or W is expected, checkcast is added.

interface X: U, W
interface Z: U, W

interface U
interface W

interface A : X, Z
interface B : X, Z

fun interface IFoo<T> where T : U, T : W {
    fun accept(t: T)
}

fun <T> sel(x: T, y: T) = x

class G<T> where T: U, T: W {
    fun check(x: IFoo<in T>) {}
}

fun test() {
    val g = sel(G<A>(), G<B>())
    g.check {} // TODO: report a compile time error for this case
}
