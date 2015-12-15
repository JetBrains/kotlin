// !DIAGNOSTICS: -UNUSED_PARAMETER
class A {
    companion object {
        fun foo(): Int = 1
        val prop = 2
        val C = 3
    }
    object B {
        fun bar(): Int = 4
        val prop = 5
    }
    object C {
    }

    constructor(x: Int)
    constructor() : this(foo() + prop + B.bar() + B.prop <!NONE_APPLICABLE!>+<!> C)
}
