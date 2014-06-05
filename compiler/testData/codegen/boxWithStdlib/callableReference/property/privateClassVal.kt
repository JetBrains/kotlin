class Result {
    private val value = "OK"

    fun ref(): KMemberProperty<Result, String> = ::value
}

fun box(): String {
    val p = Result().ref()
    try {
        p.get(Result())
        return "Fail: private property is accessible by default"
    } catch(e: IllegalAccessException) { }

    return "OK"
}
