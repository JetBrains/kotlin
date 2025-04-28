// RUN_PIPELINE_TILL: FRONTEND
interface A {
    fun foo(a: Int = 1): Int
}

interface B {
    fun foo(a: Int = 2): Int
}

fun bar(): Int {
    val o = object : B, A {
        override fun foo(<!MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES!>a: Int<!>) = a
    }
    return o.foo()
}
