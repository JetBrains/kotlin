// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters
class A
class B

open class Base {
    context(a: A, b: B)
    open fun foo() { }

    context(a: A, b: B)
    open val b: String
        get() = "2"
}

class Test1 : Base() {
    context(b: B, a: A)
    <!NOTHING_TO_OVERRIDE!>override<!> fun foo() {}

    context(b: B, a: A)
    <!NOTHING_TO_OVERRIDE!>override<!> val b: String
        get() = "2"
}

class Test2 : Base() {
    context(a: A)
    <!NOTHING_TO_OVERRIDE!>override<!> fun foo() {}

    context(a: A)
    <!NOTHING_TO_OVERRIDE!>override<!> val b: String
        get() = "2"
}

class Test3 : Base() {
    context(a: A)
    <!NOTHING_TO_OVERRIDE!>override<!> fun foo(b: B) {}
}

class Test4 : Base() {
    context(a: A)
    <!NOTHING_TO_OVERRIDE!>override<!> fun B.foo() {}

    context(a: A)
    <!NOTHING_TO_OVERRIDE!>override<!> val B.b: String
        get() = "2"
}

class Test5 : Base() {
    context(a: A, b: String)
    <!NOTHING_TO_OVERRIDE!>override<!> fun foo() {}

    context(a: A, b: String)
    <!NOTHING_TO_OVERRIDE!>override<!> val b: String
        get() = "2"
}