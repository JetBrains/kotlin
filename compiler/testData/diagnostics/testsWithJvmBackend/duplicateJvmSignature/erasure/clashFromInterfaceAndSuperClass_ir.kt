// !DIAGNOSTICS: -UNUSED_PARAMETER
// TARGET_BACKEND: JVM_IR

public open class A<T> {
    fun foo(x: T) = "O"
    fun foo(x: A<T>) = "K"
}

interface C<E> {
    fun foo(x: E): String
    fun foo(x: A<E>): String
}

interface D {
    fun foo(x: A<String>): String
}

class <!CONFLICTING_INHERITED_JVM_DECLARATIONS!>B1<!> : A<A<String>>(), D

interface <!CONFLICTING_INHERITED_JVM_DECLARATIONS!>B2<!> : C<A<String>>, D
