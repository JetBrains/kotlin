// IS_APPLICABLE: false
open class A

open class B : A() {
    fun b() {}
}

fun test() {
    val b = B()
    if (<caret>b !is A) {
        return
    }
    b.b()
}