// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// DISABLE_NEXT_PHASE_SUGGESTION
// ISSUE: KT-76436
open class A<T> {
    <!NOTHING_TO_INLINE!>inline<!> fun foo(t: T) {
    }
}

class B : A<(String) -> String>() {}

fun main() {
    B().foo { <!RETURN_NOT_ALLOWED!>return<!> }
}
