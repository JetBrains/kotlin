fun box(): String {
    val s = "OK"

    [inline] fun localFun(): String {
        return s
    }

    return localFun()
}