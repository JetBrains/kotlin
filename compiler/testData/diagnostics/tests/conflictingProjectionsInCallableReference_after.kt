// ISSUE: KT-84405
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ProperSupportOfInnerClassesInCallableReferenceLHS

// FILE: simple/file1.kt

package simple

class A<in T> {
    fun foo() {}
}

class B<out T> {
    fun foo() {}
}

fun test() {
    A<<!CONFLICTING_PROJECTION!>out<!> String>::foo
    A<<!REDUNDANT_PROJECTION!>in<!> String>::foo
    B<<!REDUNDANT_PROJECTION!>out<!> String>::foo
    B<<!CONFLICTING_PROJECTION!>in<!> String>::foo

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
    TA<<!REDUNDANT_PROJECTION!>in<!> String>::foo
    TA<<!CONFLICTING_PROJECTION_IN_TYPEALIAS_EXPANSION!>out<!> String>::foo
    TA<String>::foo
    TA<*>::foo

    TB<<!CONFLICTING_PROJECTION_IN_TYPEALIAS_EXPANSION!>in<!> String>::foo
    TB<<!REDUNDANT_PROJECTION!>out<!> String>::foo
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
    A<<!REDUNDANT_PROJECTION!>in<!> String, <!REDUNDANT_PROJECTION!>out<!> Int>::foo
    A<<!CONFLICTING_PROJECTION!>out<!> String, <!CONFLICTING_PROJECTION!>in<!> Int>::foo
    A<<!REDUNDANT_PROJECTION!>in<!> String, <!CONFLICTING_PROJECTION!>in<!> Int>::foo
}

// FILE: inner/local/file5.kt

package inner.local

fun <X> test() {
    class A<in T> {
        inner class B<out S> {
            fun test() {
                B<<!CONFLICTING_PROJECTION!>in<!> String>::foo
                B<<!REDUNDANT_PROJECTION!>out<!> String>::foo
            }
            fun foo() { }
        }
    }
    A<<!REDUNDANT_PROJECTION!>in<!> String>.B<String>::foo
    A<<!CONFLICTING_PROJECTION!>out<!> String>.B<String>::foo
}

// FILE: two/type/aliases/file6.kt

package two.type.aliases

class C<in X> {
    fun foo() { }
}

typealias T1<X> = C<X>
typealias T2<X> = T1<X>

fun test() {
    T2<<!CONFLICTING_PROJECTION_IN_TYPEALIAS_EXPANSION!>out<!> String>::foo
    T2<<!REDUNDANT_PROJECTION!>in<!> Int>::foo
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, functionDeclaration, in, inProjection, inner, localClass,
nullableType, out, outProjection, starProjection, typeAliasDeclaration, typeAliasDeclarationWithTypeParameter,
typeParameter */
