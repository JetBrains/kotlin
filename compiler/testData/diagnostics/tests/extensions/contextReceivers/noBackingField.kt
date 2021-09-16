// !LANGUAGE: +ContextReceivers

interface A {
    fun a(): Int
}
interface B {
    fun b(): Int
}

context(A)
val a = <!CONTEXT_RECEIVERS_WITH_BACKING_FIELD!>1<!>

context(A, B)
var b = <!CONTEXT_RECEIVERS_WITH_BACKING_FIELD!>2<!>

context(A, B)
val c get() = a() + b()