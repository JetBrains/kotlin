// IGNORE_BACKEND: WASM
// WITH_STDLIB

interface I1
interface I2 {
    val z: Int
}

open class Base<T : I1> {
    open fun foo(x: T): I1 = x
}

open class Derived<T> : Base<T>() where T : I1, T: I2 {
    override fun foo(x: T): I1 {
        println(x.z)
        return x
    }
}

class C : I1

class D : I1, I2 {
    override val z = 42
}

fun box(): String {
    try {
        val b = Derived<D>()
        val i1 = (b as Base<C>).foo(C())
        return "FAIL: $i1"
    } catch (e: ClassCastException) {
        return "OK"
    }
}
