// RUN_PIPELINE_TILL: FRONTEND
// FILE: main.kt
interface A {
    suspend <!CONFLICTING_OVERLOADS!>fun foo()<!>
    <!CONFLICTING_OVERLOADS!>fun foo()<!>
}

interface B : A {
    suspend override <!CONFLICTING_OVERLOADS!>fun <!CONFLICTING_INHERITED_MEMBERS!>foo<!>()<!> {

    }

    override <!CONFLICTING_OVERLOADS!>fun <!CONFLICTING_INHERITED_MEMBERS!>foo<!>()<!> {

    }
}
