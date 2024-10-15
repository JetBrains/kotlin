// RUN_PIPELINE_TILL: SOURCE
class A {
    val number: Number
        <!UNSUPPORTED_FEATURE!>field = 1<!>
}
