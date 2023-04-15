// FIR_IDENTICAL
// ISSUE: KT-57880

fun outerLambda(action: String.() -> Unit) {}
var lambda: Int.() -> Unit = {}
fun consume(arg: String) {}
fun consumeInt(arg: Int) {}

fun main() {
    outerLambda {
        lambda = {
            consume(this@outerLambda)
        }
    }

    outerLambda {
        lambda = innerLambda@{
            consumeInt(this@innerLambda)
        }
    }
}
