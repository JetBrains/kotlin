// RUN_PIPELINE_TILL: BACKEND

fun test(l: List<String>) {
    for (<!WRONG_MODIFIER_TARGET!>lateinit<!> x in l) {}
}
