class MyThrowable(message: String? = null, cause: Throwable? = null) : Throwable(message, cause) {

    override val message: String?
        get() = "My message: " + super.message

    override val cause: Throwable?
        get() = super.cause ?: this

}

fun box(): String {
    try {
        throw MyThrowable("test")
    } catch (t: MyThrowable) {
        if (t.cause != t) return "fail t.cause"
        if (t.message != "My message: test") return "fail t.message"
        return "OK"
    }

    return "fail: MyThrowable wasn't caught."
}
