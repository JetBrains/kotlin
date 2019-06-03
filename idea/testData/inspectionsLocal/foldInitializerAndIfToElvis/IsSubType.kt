open class A

open class B : A() {
    fun b() {}
}

open class C : B() {
    fun c() {}
}

fun test() {
    val b = B()
    if (<caret>b !is C) {
        return
    }
    b.b()
}