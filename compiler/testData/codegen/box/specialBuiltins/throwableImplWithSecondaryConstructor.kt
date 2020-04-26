class MyThrowable : Throwable {
    val x: String

    constructor(x: String, message: String, cause: Throwable? = null) : super(x + message, cause) {
        this.x = x
    }
}

fun box(): String {
    try {
        throw MyThrowable("O", "K")
    }
    catch (t: MyThrowable) {
        if (t.cause != null) return "fail t.cause"
        if (t.message != "OK") return "fail t.message: ${t.message}"
        if (t.x != "O") return "fail t.x: ${t.x}"
        return "OK"
    }

    return "fail: MyThrowable wasn't caught."
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: IR_TRY
