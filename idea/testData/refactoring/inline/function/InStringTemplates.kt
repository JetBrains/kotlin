class X(val v: Int) {
    fun <caret>foo() {
        println("foo()")
        return v
    }
}

fun X.f1() {
    println("Value: ${foo()}")
}

fun f2(x: X) {
    println("Value: ${x.foo()}")
}