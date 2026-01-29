interface X: U, W
interface Z: U, W


interface U {
    fun foo() = "O"
}
interface W {
    fun bar() = "K"
}

interface A : X, Z
interface B : X, Z

fun interface IFoo<T> where T : U, T : W {
    fun accept(t: T)
}

fun <T> sel(x: T, y: T) = x

class G<T>(private val y: T) where T: U, T: W {
    fun check(x: IFoo<in T>) {
        x.accept(y)
    }
}

fun box(): String {
    val g = sel(G<A>(object : A{}), G<B>(object : B {}))
    var s = ""
    g.check {
        s += it.foo()
        s += it.bar()
    } // TODO: report a compile time error for this case
    return s
}