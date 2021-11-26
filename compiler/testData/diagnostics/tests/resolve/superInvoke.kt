// FIR_IDENTICAL
// SKIP_TXT
// FILE: main.kt

open class A {
    protected open val x: (String) -> Boolean = { true }
}

class B : A() {
    override val x = { y: String ->
        super.x(y)
    }
}
