class A {
    fun b() = B()
}

class B {
    operator fun invoke(f: () -> Unit) {}
}

fun test(a: A) {
    a.b().<caret>invoke {
    }
}