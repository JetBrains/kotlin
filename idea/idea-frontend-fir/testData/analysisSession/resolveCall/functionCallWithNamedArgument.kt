fun function(a: Int, b: String) {}

fun call() {
    <expr>function(b = "foo", a = 1)</expr>
}

// CALL: KtFunctionCall: targetFunction = /function(a: kotlin.Int, b: kotlin.String): kotlin.Unit