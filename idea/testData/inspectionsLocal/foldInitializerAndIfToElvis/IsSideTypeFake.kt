// PROBLEM: none
interface A {
    fun a() {}
}

open class B : A {
    fun b() {}
}

interface C : A {
    fun c() {}
}

fun test() {
    val b = B()
    if (<caret>b !is C) {
        return
    }
    b.b()
    b.c()
}