// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_VARIABLE
package test

object Wrong
object Right

class a {
    class b<T> {
        class c {
            fun foo() = Wrong
        }
    }
}

fun Int.foo() = Right

class Test {
    val a: List<Int> = null!!

    val <T> List<T>.b: Int get() = 42

    val Int.c: Int get() = 42

    val test1: () -> Right = a.b.c::foo
    val test1a: () -> Right = a.b<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><Int><!>.c::foo

    val test2: () -> Right = <!SAFE_CALLABLE_REFERENCE_CALL!>a.b.c?::foo<!>
    val test2a: () -> Right = <!SAFE_CALLABLE_REFERENCE_CALL!>a.b<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!><Int><!>.c?::foo<!>
}

/* GENERATED_FIR_TAGS: callableReference, checkNotNullCall, classDeclaration, funWithExtensionReceiver,
functionDeclaration, functionalType, getter, integerLiteral, nestedClass, nullableType, objectDeclaration,
propertyDeclaration, propertyWithExtensionReceiver, typeParameter */
