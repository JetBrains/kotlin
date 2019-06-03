open class A

open class B : A() {
    fun b() {}
}

fun test() {
    val b = B()
    if (<caret>b !is B) {
        return
    }
    b.b()
}