// K1 generates `accept(Object)` and bridge `accept(X)` in the SAM adapter.
// K2 generates `accept(X)` only. It is fine though, because if `check`'s parameter is passed where Z is expected, checkcast is added.

interface X
interface Z

interface A : X, Z
interface B : X, Z

fun interface IFoo<T> where T : X, T : Z {
    fun accept(t: T)
}

fun <T> sel(x: T, y: T) = x

class G<T> where T: X, T: Z {
    fun check(x: IFoo<in T>) {}
}

fun test() {
    val g = sel(G<A>(), G<B>()) // g: G<out { X & Z }>
    g.check {} // (*) target SAM type: IFoo<{ X & Z }> (TODO: report a compile time error for this case)
}
