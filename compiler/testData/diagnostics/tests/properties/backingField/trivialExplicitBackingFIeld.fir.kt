// RUN_PIPELINE_TILL: FRONTEND
class A {
    val number: Number
        <!UNSUPPORTED_FEATURE!>field = 1<!>
}
