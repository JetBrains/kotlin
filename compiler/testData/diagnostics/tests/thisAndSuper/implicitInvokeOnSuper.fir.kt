// !DIAGNOSTICS: -UNUSED_PARAMETER

open class A {
    operator fun invoke() {}
    operator fun invoke(f: () -> Unit) {}
}

class B : A() {
    fun bar() {
        super()
        (super)()
        super {}
        (super) {}
    }
}