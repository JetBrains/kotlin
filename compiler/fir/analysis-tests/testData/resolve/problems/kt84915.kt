// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-84915
// WITH_STDLIB

// KT-84915: "Expected expression 'FirAnonymousFunctionExpressionImpl' to be resolved" when using lambda in RHS of equality operator in PCLA

interface Box<T> {
    var t: T
}

fun <T: Any> buildBox(block: Box<T>.() -> Unit): Box<T> =
    object : Box<T> { override lateinit var t: T }.apply(block)

fun main() {
    buildBox {
        t == {}
    }
}
