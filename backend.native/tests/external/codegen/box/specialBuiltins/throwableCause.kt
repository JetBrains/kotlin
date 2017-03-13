fun box(): String {
    var t = Throwable("O", Throwable("K"))
    if (t.message != "O" || t.cause?.message != "K") return "fail1"

    t = Throwable(Throwable("OK"))
    if (t.message == null || t.message == "OK" || t.cause?.message != "OK") return "fail2"

    t = Throwable("OK")
    if (t.message != "OK" || t.cause != null) return "fail3"

    t = Throwable()
    if (t.message != null || t.cause != null) return "fail4"

    return "OK"
}
