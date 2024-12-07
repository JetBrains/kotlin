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
