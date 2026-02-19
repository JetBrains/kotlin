// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -NOTHING_TO_INLINE, -UNREACHABLE_CODE
// LANGUAGE: -ForbidExposingLessVisibleTypesInInline

private interface Private

internal inline fun internal(arg: Any): Boolean = arg is Private // should be an error

open class C {
    protected class Protected

    internal inline fun internal(arg: Any): Boolean = arg is Protected // should be an error
    internal inline fun internal2(): Any = Protected() // should be an error
}

fun <T> ignore() {}

internal inline fun internal() {
    ignore<Private>() // should be an error
    Private::class
}

private class Private2 {
    object Obj
    fun foo() {}
}

internal inline fun internal2() {
    ignore<Private2.Obj>() // should be an error
    <!PRIVATE_CLASS_MEMBER_FROM_INLINE!>Private2<!>().<!PRIVATE_CLASS_MEMBER_FROM_INLINE!>foo<!>()
}

private fun <T : Private> private1(arg: () -> T) {}

private fun private2(): List<Private> = TODO()

private fun private3(arg: Private) {}

private val value = object {
    fun foo() {}
}

private var varProp: Private? = null

internal inline fun internal3() {
    private1 { null!! } // should be an error
    private2() // should be an error
    private3(null!!) // should be an error
    value // should be an error (anonymous type)
    varProp = null
}

private class A {
    class B {
        companion object {
            fun foo() {}
        }
    }
}

internal inline fun internal4() {
    A.B.foo()// should be an error
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
        <!PRIVATE_CLASS_MEMBER_FROM_INLINE!>foo<!>() // already an error, should be an error
        Companion
        C3
        C3TA
    }
}

internal inline fun withAnonymousObject() {
    object {
        private inner <!NOT_YET_SUPPORTED_IN_INLINE!>class<!> Inner {}
        fun foo(x: Any) {
            Inner()
            x is Inner
        }
    }.foo("")
}

private fun foo() = object { fun bar() {} }
internal inline fun test() = foo().bar()

private object O {
    class C
}

internal inline fun internal5() {
    O.C()
}

private fun interface I {
    fun foo(): Int
}

internal inline fun internal6(): Int = (I { 1 }).<!PRIVATE_CLASS_MEMBER_FROM_INLINE!>foo<!>()

/* GENERATED_FIR_TAGS: anonymousObjectExpression, assignment, checkNotNullCall, classDeclaration, classReference,
companionObject, functionDeclaration, functionalType, inline, inner, integerLiteral, interfaceDeclaration, isExpression,
lambdaLiteral, localClass, nestedClass, nullableType, objectDeclaration, propertyDeclaration, stringLiteral,
typeConstraint, typeParameter */
