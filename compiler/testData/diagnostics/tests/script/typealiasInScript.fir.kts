// RUN_PIPELINE_TILL: FRONTEND
typealias TopLevelInScript = String

class C {
    <!UNSUPPORTED_FEATURE!>typealias NestedInClass = String<!>
}

fun foo() {
    <!UNSUPPORTED!>typealias Local = String<!>
}
