// RUN_PIPELINE_TILL: FRONTEND
fun main(args: Array<String>) {
    val x = 42
    when (x) {is<!SYNTAX!><!>}
}
