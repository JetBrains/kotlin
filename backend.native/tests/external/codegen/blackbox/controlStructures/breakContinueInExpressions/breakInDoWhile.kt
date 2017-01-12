fun box(): String {
    val ok: String? = "OK"
    var res = ""

    do {
        res += ok ?: break
    } while (false)

    return res
}