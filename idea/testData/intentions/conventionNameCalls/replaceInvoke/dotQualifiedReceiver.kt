class A {
    val b = B()
}

class B {
    operator fun invoke() {}
}

fun test(a: A) {
    a.b.<caret>invoke()
}