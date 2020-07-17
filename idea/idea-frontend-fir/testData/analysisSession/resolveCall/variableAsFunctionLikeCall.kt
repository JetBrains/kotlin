operator fun Int.invoke(): String {}

fun call(x: kotlin.int) {
    <selection>x()</selection>
}

// CALL: FunctionCallInfo: targetFunction = invoke(<receiver>: kotlin.Int): kotlin.String