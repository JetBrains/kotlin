
interface X
interface Z

interface A : X, Z
interface B : X, Z

fun interface IFoo<T: X> {
    fun accept(t: T)
}

fun <T> sel(x: T, y: T) = x

class G<T: X> {
    fun check(x: IFoo<in T>) {}
}

fun box(): String {
    val g = sel(G<A>(), G<B>()) // g: G<out { X & Z }>
    g.check {} // (*) target SAM type: IFoo<X>
    return "OK"
}