// RUN_PIPELINE_TILL: SOURCE
enum class E {
    A,
    B,
    C
}

fun foo() {
    val e = E.<!SYNTAX!><!>
}


