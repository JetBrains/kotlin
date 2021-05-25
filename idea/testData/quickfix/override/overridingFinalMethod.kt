// "Make A.foo open" "true"
open class A {
    fun foo() {}
}

class B : A() {
    override<caret> fun foo() {}
}
/* IGNORE_FIR */
