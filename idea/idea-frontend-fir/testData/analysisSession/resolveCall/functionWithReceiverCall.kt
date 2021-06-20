fun String.function(a: Int) {}

fun call() {
   "str".<expr>function(1)</expr>
}

// CALL: KtFunctionCall: targetFunction = /function(<receiver>: kotlin.String, a: kotlin.Int): kotlin.Unit