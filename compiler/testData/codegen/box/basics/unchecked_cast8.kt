// FREE_COMPILER_ARGS: -Xbinary=genericSafeCasts=true
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

open class Base2<in T : I1> {
    open fun foo(x: T): I1 = x
}

open class Derived2<in T> : Base2<T>() where T : I1, T: I2 {
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
        return "FAIL 1: $i1"
    } catch (e: ClassCastException) {}
    try {
        val b = Derived2<D>()
        val i1 = (b as Base2<C>).foo(C())
        return "FAIL 2: $i1"
    } catch (e: ClassCastException) {}
    return "OK"
}
