// FIR_IDENTICAL
fun use1(fn: (A, Int) -> Unit) {}

fun use2(fn: (Int) -> Unit) {}

open class A {
    open fun foo(vararg xs: Int) = 1
}

object Obj : A() {
    override fun foo(vararg xs: Int) = 1
}

fun testUnbound() {
    use1(A::foo)
}

fun testBound(a: A) {
    use2(a::foo)
}

fun testObject() {
    use2(Obj::foo)
}
