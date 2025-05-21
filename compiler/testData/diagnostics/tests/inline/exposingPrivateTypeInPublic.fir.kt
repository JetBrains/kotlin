// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -NOTHING_TO_INLINE, -UNREACHABLE_CODE
// LANGUAGE: -ForbidExposingLessVisibleTypesInInline

private interface Private

inline fun internal(arg: Any): Boolean = arg is <!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_WARNING!>Private<!> // should be an error

open class C {
    protected class Protected

    inline fun internal(arg: Any): Boolean = arg is <!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_WARNING!>Protected<!> // should be an error
    inline fun internal2(): Any = <!PROTECTED_CONSTRUCTOR_CALL_FROM_PUBLIC_INLINE!>Protected<!>() // should be an error
}

fun <T> ignore() {}

inline fun internal() {
    ignore<<!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_WARNING!>Private<!>>() // should be an error
    <!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_WARNING!>Private<!>::class
}

private class Private2 {
    object Obj
}

inline fun internal2() {
    ignore<<!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_WARNING!>Private2.Obj<!>>() // should be an error
}

private fun <T : Private> private1(arg: () -> T) {}

private fun private2(): List<Private> = TODO()

private fun private3(arg: Private) {}

private val value = object {
    fun foo() {}
}

private var varProp: Private? = null

inline fun internal3() {
    <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>private1<!> { null!! } // should be an error
    <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>private2<!>() // should be an error
    <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>private3<!>(null!!) // should be an error
    <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>value<!> // should be an error (anonymous type)
    <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE, NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>varProp<!> = null
}

private class A {
    class B {
        companion object {
            fun foo() {}
        }
    }
}

inline fun internal4() {
    A.<!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_WARNING!>B<!>.<!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>foo<!>()// should be an error
}

class C2 {
    private val value = 4
    companion object {
        private fun foo() {}
    }

    inline fun internal() {
        <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>value<!> // ok
        <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>foo<!>() // ok
    }
}

class C3 {
    private companion object {
        fun foo() {}
    }

    inline fun internal() {
        <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>foo<!>() // already an error, should be an error
    }
}
