operator fun Int.invoke(): String {}

fun call(x: kotlin.int) {
    <expr>x()</expr>
}

// CALL: KtFunctionCall: targetFunction = /invoke(<receiver>: kotlin.Int): kotlin.String