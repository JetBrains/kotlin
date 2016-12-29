fun box(): String {
    var t = Throwable("O", Throwable("K"))
    if (t.message != "O" || t.cause?.message != "K") return "fail1"

    t = Throwable(Throwable("OK"))
    if (t.message != null || t.cause?.message != "OK") return "fail2"

    return "OK"
}
