fun String.function(a: Int) {}

fun call() {
    "str"?.<selection>function(1)</selection>
}

// CALL: FunctionCallInfo: targetFunction = function(<receiver>: kotlin.String, a: kotlin.Int): kotlin.Unit