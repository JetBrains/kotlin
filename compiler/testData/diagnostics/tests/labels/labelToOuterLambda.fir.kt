// ISSUE: KT-57880

fun outerLambda(action: String.() -> Unit) {}
var lambda: Int.() -> Unit = {}
fun consume(arg: String) {}

fun main() {
    outerLambda {
        lambda = {
            consume(<!ARGUMENT_TYPE_MISMATCH!>this@outerLambda<!>)
        }
    }
}
