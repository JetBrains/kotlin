// ISSUE: KT-84405
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -ProperSupportOfInnerClassesInCallableReferenceLHS

// FILE: simple/file1.kt

package simple

class A<in T> {
    fun foo() {}
}

class B<out T> {
    fun foo() {}
}

fun test() {
    A<out String>::foo
    A<in String>::foo
    B<out String>::foo
    B<in String>::foo

    A<*>::foo
    B<*>::foo

    A<String>::foo
    B<String>::foo
}

// FILE: with/type/alias/file2.kt

package with.type.alias

class A<in T> {
    fun foo() { }
}
typealias TA<X> = A<X>

class B<out T> {
    fun foo() { }
}
typealias TB<X> = B<X>

fun test() {
    TA<in String>::foo
    TA<out String>::foo
    TA<String>::foo
    TA<*>::foo

    TB<in String>::foo
    TB<out String>::foo
    TB<String>::foo
    TB<*>::foo
}

// FILE: nested/file3.kt

package nested

class A<in T> {
    fun foo() { }
}

class B<out T> {
    fun foo() { }
}

fun test() {
    A<B<<!REDUNDANT_PROJECTION!>out<!> String>>::foo
    A<B<<!CONFLICTING_PROJECTION!>in<!> String>>::foo
    A<B<String>>::foo
    A<B<*>>::foo

    B<A<<!CONFLICTING_PROJECTION!>out<!> String>>::foo
    B<A<<!REDUNDANT_PROJECTION!>in<!> String>>::foo
    B<A<String>>::foo
    B<A<*>>::foo
}

// FILE: many/arguments/file4.kt

package many.arguments

class A<in T, out S> {
    fun foo() { }
}

fun test() {
    A<in String, out Int>::foo
    A<out String, in Int>::foo
    A<in String, in Int>::foo
}

// FILE: inner/local/file5.kt

package inner.local

fun <X> test() {
    class A<in T> {
        inner class B<out S> {
            fun test() {
                <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>B<in String><!>::foo
                <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>B<out String><!>::foo
            }
            fun foo() { }
        }
    }
    A<in String>.B<String>::foo
    A<out String>.B<String>::foo
}

// FILE: two/type/aliases/file6.kt

package two.type.aliases

class C<in X> {
    fun foo() { }
}

typealias T1<X> = C<X>
typealias T2<X> = T1<X>

fun test() {
    T2<out String>::foo
    T2<in Int>::foo
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, functionDeclaration, in, inProjection, inner, localClass,
nullableType, out, outProjection, starProjection, typeAliasDeclaration, typeAliasDeclarationWithTypeParameter,
typeParameter */
