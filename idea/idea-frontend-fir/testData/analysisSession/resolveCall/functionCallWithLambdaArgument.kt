fun function(a: Int, b: (String) -> Boolean) {}

fun call() {
    <expr>function(1) { s -> true }</expr>
}

// CALL: KtFunctionCall: targetFunction = /function(a: kotlin.Int, b: (kotlin.String) -> kotlin.Boolean): kotlin.Unit