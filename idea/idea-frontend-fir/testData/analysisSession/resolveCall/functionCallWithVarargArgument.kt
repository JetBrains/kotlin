fun function(vararg a: Int) {}

fun call() {
    <expr>function(1, 2, 3)</expr>
}

// CALL: KtFunctionCall: targetFunction = /function(vararg a: kotlin.IntArray): kotlin.Unit