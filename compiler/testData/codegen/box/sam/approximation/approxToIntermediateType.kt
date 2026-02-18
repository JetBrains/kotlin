interface X {
    fun foo() = "O"
}
interface Z {
    fun bar() = "K"
}

interface W : X, Z

interface A : W
interface B : W

fun interface IFoo<T> where T : X, T : Z {
    fun accept(t: T)
}

fun <T> sel(x: T, y: T) = x

class G<T>(private val y: T) where T: X, T: Z {
    fun check(x: IFoo<in T>) {
        x.accept(y)
    }
}

fun box(): String {
    val g = sel(G<A>(object : A{}), G<B>(object : B {})) // g: G<out { X & Z }>
    var s = ""
    g.check {
        s += it.foo()
        s += it.bar()
    } // target SAM type: IFoo<W>
    return s
}