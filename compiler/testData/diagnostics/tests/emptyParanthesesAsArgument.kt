// RUN_PIPELINE_TILL: FRONTEND
fun f(a: Int, b: Int, c: Int) {}

fun main() {
    f(c = 3, (<!SYNTAX!><!>), <!NO_VALUE_FOR_PARAMETER!>a = 1)<!>
}