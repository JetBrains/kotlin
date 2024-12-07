// RUN_PIPELINE_TILL: FRONTEND
// FILE: main.kt
interface A {
    <!CONFLICTING_OVERLOADS!>suspend fun foo()<!>
    <!CONFLICTING_OVERLOADS!>fun foo()<!>
}

interface B : A {
    <!CONFLICTING_OVERLOADS, CONFLICTING_OVERLOADS!>suspend override fun foo()<!> {

    }

    <!CONFLICTING_OVERLOADS, CONFLICTING_OVERLOADS!>override fun foo()<!> {

    }
}
