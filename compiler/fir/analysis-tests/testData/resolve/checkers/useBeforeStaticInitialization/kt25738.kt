// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
sealed class S {
    object O : S()

    companion object {
        <!UNINITIALIZED_PROPERTY!>val x = foo(<!UNINITIALIZED_ACCESS!>O<!>)<!>
    }
}

fun foo(o: S) = 42
