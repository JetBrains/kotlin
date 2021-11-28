interface X
interface Z

interface W : X, Z

interface A : W
interface B : W

fun interface IFoo<T> where T : X, T : Z {
    fun accept(t: T)
}

fun <T> sel(x: T, y: T) = x

class G<T> where T: X, T: Z {
    fun check(x: IFoo<in T>) {}
}

fun box(): String {
    val g = sel(G<A>(), G<B>()) // g: G<out { X & Z }>
    g.check {} // target SAM type: IFoo<W>
    return "OK"
}