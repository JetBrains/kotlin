// FILE: main.kt
interface A {
    suspend <!CONFLICTING_OVERLOADS!>fun foo()<!>
    <!CONFLICTING_OVERLOADS!>fun foo()<!>
}

interface B : A {
    suspend override <!CONFLICTING_OVERLOADS!>fun foo()<!> {

    }

    override <!CONFLICTING_OVERLOADS!>fun foo()<!> {

    }
}
