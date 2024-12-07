// RUN_PIPELINE_TILL: FRONTEND
open class A {
    open var it: Number
        private field = 3
        set(value) {
            field = value.toInt()
        }

    fun test() {
        // error, because `it` is not
        // final, so no smart type narrowing
        // is provided
        println(it <!UNRESOLVED_REFERENCE!>+<!> 1)
    }
}

open class B : A() {
    override var it: Number
        get() = 3.14
        set(value) {}
}
