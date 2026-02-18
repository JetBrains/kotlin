// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -NOTHING_TO_INLINE, -UNREACHABLE_CODE
// LANGUAGE: -ForbidExposingLessVisibleTypesInInline

private interface Private

inline fun public(arg: Any): Boolean = arg is Private // should be an error

open class C {
    protected class Protected

    inline fun public(arg: Any): Boolean = arg is Protected // should be an error
    inline fun public2(): Any = <!PROTECTED_CONSTRUCTOR_CALL_FROM_PUBLIC_INLINE_ERROR!>Protected<!>() // should be an error
}

fun <T> ignore() {}

inline fun public() {
    ignore<Private>() // should be an error
    Private::class
}

private class Private2 {
    object Obj
}

inline fun public2() {
    ignore<Private2.Obj>() // should be an error
}

private fun <T : Private> private1(arg: () -> T) {}

private fun private2(): List<Private> = TODO()

private fun private3(arg: Private) {}

private val value = object {
    fun foo() {}
}

private var varProp: Private? = null

inline fun public3() {
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

inline fun public4() {
    A.B.<!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>foo<!>()// should be an error
}

class C2 {
    private val value = 4
    companion object {
        private fun foo() {}
    }

    inline fun public() {
        <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>value<!> // ok
        <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>foo<!>() // ok
    }
}

typealias C3TA = C3

class C3 {
    private companion object {
        fun foo() {}
    }

    inline fun public() {
        <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>foo<!>() // already an error, should be an error
        <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>Companion<!>
        <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>C3<!>
        <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>C3TA<!>
    }
}

private object O {
    class C
}

inline fun public5() {
    O.<!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>C<!>()
}

private fun interface I {
    fun foo(): Int
}

inline fun public6(): Int = (<!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>I<!> { 1 }).<!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>foo<!>()

/* GENERATED_FIR_TAGS: anonymousObjectExpression, assignment, checkNotNullCall, classDeclaration, classReference,
companionObject, functionDeclaration, functionalType, inline, integerLiteral, interfaceDeclaration, isExpression,
lambdaLiteral, nestedClass, nullableType, objectDeclaration, propertyDeclaration, typeConstraint, typeParameter */
