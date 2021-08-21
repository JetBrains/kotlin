fun function(vararg a: Int) {}

fun call() {
    val args = intArrayOf(1, 2, 3)
    <expr>function(*args)</expr>
}

// CALL: KtFunctionCall: targetFunction = /function(vararg a: kotlin.IntArray): kotlin.Unit