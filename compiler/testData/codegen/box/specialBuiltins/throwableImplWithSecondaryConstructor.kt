// IGNORE_BACKEND_FIR: JVM_IR
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
