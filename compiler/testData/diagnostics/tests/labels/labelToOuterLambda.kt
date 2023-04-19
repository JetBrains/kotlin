// FIR_IDENTICAL
// ISSUE: KT-57880, KT-58076
// DIAGNOSTICS: -UNUSED_VARIABLE, -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE

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

    lateinit var c: Int.() -> Unit
    val a = "hello".apply {
        val b: Int.() -> Unit = {
            this@apply.hello()
        }
        c = {
            this@apply.hello()
        }
    }
}

fun String.hello() = this
