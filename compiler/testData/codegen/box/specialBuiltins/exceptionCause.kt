class CustomException : Throwable {
    constructor(message: String?, cause: Throwable?) : super(message, cause)

    constructor(message: String?) : super(message, null)

    constructor(cause: Throwable?) : super(cause)

    constructor() : super()
}

fun box(): String {
    var t = CustomException("O", Throwable("K"))
    if (t.message != "O" || t.cause?.message != "K") return "fail1"

    t = CustomException(Throwable("OK"))
    if (t.message == null || t.message == "OK" || t.cause?.message != "OK") return "fail2"

    t = CustomException("OK")
    if (t.message != "OK" || t.cause != null) return "fail3"

    t = CustomException()
    if (t.message != null || t.cause != null) return "fail4"

    return "OK"
}
