// RUN_PIPELINE_TILL: SOURCE
// FIR_IDENTICAL
class A() {
    <!CONFLICTING_OVERLOADS!>fun b()<!> {
    }

    <!CONFLICTING_OVERLOADS!>fun b()<!> {
    }
}
