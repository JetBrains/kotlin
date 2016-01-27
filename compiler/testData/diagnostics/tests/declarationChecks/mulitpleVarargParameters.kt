// !DIAGNOSTICS: -UNUSED_PARAMETER -PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED
fun test(<!MULTIPLE_VARARG_PARAMETERS!>vararg<!> x1: Int, <!MULTIPLE_VARARG_PARAMETERS!>vararg<!> x2: Int) {
    fun test2(<!MULTIPLE_VARARG_PARAMETERS!>vararg<!> x1: Int, <!MULTIPLE_VARARG_PARAMETERS!>vararg<!> x2: Int) {
        class LocalClass(<!MULTIPLE_VARARG_PARAMETERS!>vararg<!> x1: Int, <!MULTIPLE_VARARG_PARAMETERS!>vararg<!> x2: Int) {
            constructor(<!MULTIPLE_VARARG_PARAMETERS!>vararg<!> x1: Int, <!MULTIPLE_VARARG_PARAMETERS!>vararg<!> x2: Int, xx: Int) {}
        }
        fun test3(<!MULTIPLE_VARARG_PARAMETERS!>vararg<!> x1: Int, <!MULTIPLE_VARARG_PARAMETERS!>vararg<!> x2: Int) {}
    }
}

fun Any.test(<!MULTIPLE_VARARG_PARAMETERS!>vararg<!> x1: Int, <!MULTIPLE_VARARG_PARAMETERS!>vararg<!> x2: Int, <!MULTIPLE_VARARG_PARAMETERS!>vararg<!> x3: Int) {}

interface I {
    fun test(<!MULTIPLE_VARARG_PARAMETERS!>vararg<!> x1: Int, <!MULTIPLE_VARARG_PARAMETERS!>vararg<!> x2: Int)
}

abstract class C(<!MULTIPLE_VARARG_PARAMETERS!>vararg<!> x1: Int, <!MULTIPLE_VARARG_PARAMETERS!>vararg<!> x2: Int, b: Boolean) {
    fun test(<!MULTIPLE_VARARG_PARAMETERS!>vararg<!> x1: Int, <!MULTIPLE_VARARG_PARAMETERS!>vararg<!> x2: Int) {}

    abstract fun test2(<!MULTIPLE_VARARG_PARAMETERS!>vararg<!> x1: Int, <!MULTIPLE_VARARG_PARAMETERS!>vararg<!> x2: Int)

    class CC(<!MULTIPLE_VARARG_PARAMETERS!>vararg<!> x1: Int, <!MULTIPLE_VARARG_PARAMETERS!>vararg<!> x2: Int, b: Boolean) {
        constructor(<!MULTIPLE_VARARG_PARAMETERS!>vararg<!> x1: Int, <!MULTIPLE_VARARG_PARAMETERS!>vararg<!> x2: Int) {}
        fun test(<!MULTIPLE_VARARG_PARAMETERS!>vararg<!> x1: Int, <!MULTIPLE_VARARG_PARAMETERS!>vararg<!> x2: Int) {}
    }
}

object O {
    fun test(<!MULTIPLE_VARARG_PARAMETERS!>vararg<!> x1: Int, <!MULTIPLE_VARARG_PARAMETERS!>vararg<!> x2: Int) {}

    class CC(<!MULTIPLE_VARARG_PARAMETERS!>vararg<!> x1: Int, <!MULTIPLE_VARARG_PARAMETERS!>vararg<!> x2: Int, b: Boolean) {
        constructor(<!MULTIPLE_VARARG_PARAMETERS!>vararg<!> x1: Int, <!MULTIPLE_VARARG_PARAMETERS!>vararg<!> x2: Int) {}
        fun test(<!MULTIPLE_VARARG_PARAMETERS!>vararg<!> x1: Int, <!MULTIPLE_VARARG_PARAMETERS!>vararg<!> x2: Int) {}
    }
}
