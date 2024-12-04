// RUN_PIPELINE_TILL: FRONTEND
enum class E {
    A,
    B,
    C
}

fun foo() {
    val e = E.<!SYNTAX!><!>
}


