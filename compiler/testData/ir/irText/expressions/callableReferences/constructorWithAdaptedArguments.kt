// FIR_IDENTICAL
fun use(fn: (Int) -> Any) = fn(42)

class C(vararg xs: Int)

class Outer {
    inner class Inner(vararg xs: Int)
}

fun testConstructor() = use(::C)

fun testInnerClassConstructor(outer: Outer) = use(outer::Inner)

fun testInnerClassConstructorCapturingOuter() = use(Outer()::Inner)
