//WITH_RUNTIME
class Test {

    data class Style(
            val color: Int? = null,
            val underlined: Boolean? = null,
            val separator: String = ""
    )

    init {
        var flag: Boolean? = null

        val receiver: String = "123"
        try {
            receiver.let { a2 ->
                flag = false
            }
        } finally {
            receiver.hashCode()
        }
        val style = Style(null, flag, "123")
    }
}


fun box(): String {
    Test()

    return "OK"
}

// DONT_TARGET_EXACT_BACKEND: WASM
//DONT_TARGET_WASM_REASON: IR_TRY