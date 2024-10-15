// RUN_PIPELINE_TILL: SOURCE
fun add(a: Int?, b: Int?): Int {
    return a+<!SYNTAX!><!>
}
