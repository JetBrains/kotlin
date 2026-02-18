// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -NOTHING_TO_INLINE, -UNREACHABLE_CODE
// LANGUAGE: -ForbidExposingLessVisibleTypesInInline

private interface Private

internal inline fun internal(arg: Any): Boolean = arg is <!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_WARNING!>Private<!> // should be an error

open class C {
    protected class Protected

    internal inline fun internal(arg: Any): Boolean = arg is <!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_WARNING!>Protected<!> // should be an error
    internal inline fun internal2(): Any = <!LESS_VISIBLE_TYPE_IN_INLINE_ACCESSED_SIGNATURE_WARNING!>Protected<!>() // should be an error
}

fun <T> ignore() {}

internal inline fun internal() {
    ignore<<!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_WARNING!>Private<!>>() // should be an error
    <!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_WARNING!>Private<!>::class
}

private class Private2 {
    object Obj
    fun foo() {}
}

internal inline fun internal2() {
    ignore<<!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_WARNING!>Private2.Obj<!>>() // should be an error
    <!LESS_VISIBLE_TYPE_IN_INLINE_ACCESSED_SIGNATURE_WARNING, PRIVATE_CLASS_MEMBER_FROM_INLINE!>Private2<!>().<!LESS_VISIBLE_TYPE_IN_INLINE_ACCESSED_SIGNATURE_WARNING, PRIVATE_CLASS_MEMBER_FROM_INLINE!>foo<!>()
}

private fun <T : Private> private1(arg: () -> T) {}

private fun private2(): List<Private> = TODO()

private fun private3(arg: Private) {}

private val value = object {
    fun foo() {}
}

private var varProp: Private? = null

internal inline fun internal3() {
    <!LESS_VISIBLE_TYPE_IN_INLINE_ACCESSED_SIGNATURE_WARNING!>private1<!> { null!! } // should be an error
    <!LESS_VISIBLE_TYPE_IN_INLINE_ACCESSED_SIGNATURE_WARNING!>private2<!>() // should be an error
    <!LESS_VISIBLE_TYPE_IN_INLINE_ACCESSED_SIGNATURE_WARNING!>private3<!>(null!!) // should be an error
    <!LESS_VISIBLE_TYPE_IN_INLINE_ACCESSED_SIGNATURE_WARNING!>value<!> // should be an error (anonymous type)
    <!LESS_VISIBLE_TYPE_IN_INLINE_ACCESSED_SIGNATURE_WARNING!>varProp<!> = null
}

private class A {
    class B {
        companion object {
            fun foo() {}
        }
    }
}

internal inline fun internal4() {
    A.B.<!LESS_VISIBLE_TYPE_IN_INLINE_ACCESSED_SIGNATURE_WARNING!>foo<!>()// should be an error
}

class C2 {
    private val value = 4
    companion object {
        private fun foo() {}
    }

    internal inline fun internal() {
        value // ok
        foo() // ok
    }
}

typealias C3TA = C3

class C3 {
    private companion object {
        fun foo() {}
    }

    internal inline fun internal() {
        <!LESS_VISIBLE_TYPE_IN_INLINE_ACCESSED_SIGNATURE_WARNING, PRIVATE_CLASS_MEMBER_FROM_INLINE!>foo<!>() // already an error, should be an error
        <!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_WARNING!>Companion<!>
        <!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_WARNING!>C3<!>
        <!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_WARNING!>C3TA<!>
    }
}

internal inline fun withAnonymousObject() {
    object {
        private inner class Inner {}
        fun foo(x: Any) {
            Inner()
            x is Inner
        }
    }.foo("")
}

private fun foo() = object { fun bar() {} }
internal inline fun test() = <!LESS_VISIBLE_TYPE_IN_INLINE_ACCESSED_SIGNATURE_WARNING!>foo<!>().bar()

private object O {
    class C
}

internal inline fun internal5() {
    <!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_WARNING!>O<!>.<!LESS_VISIBLE_TYPE_IN_INLINE_ACCESSED_SIGNATURE_WARNING!>C<!>()
}

private fun interface I {
    fun foo(): Int
}

internal inline fun internal6(): Int = (<!LESS_VISIBLE_TYPE_IN_INLINE_ACCESSED_SIGNATURE_WARNING!>I<!> { 1 }).<!LESS_VISIBLE_TYPE_IN_INLINE_ACCESSED_SIGNATURE_WARNING, PRIVATE_CLASS_MEMBER_FROM_INLINE!>foo<!>()

/* GENERATED_FIR_TAGS: anonymousObjectExpression, assignment, checkNotNullCall, classDeclaration, classReference,
companionObject, functionDeclaration, functionalType, inline, inner, integerLiteral, interfaceDeclaration, isExpression,
lambdaLiteral, localClass, nestedClass, nullableType, objectDeclaration, propertyDeclaration, stringLiteral,
typeConstraint, typeParameter */
