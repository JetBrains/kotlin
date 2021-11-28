fun stringPlus(x: String?, y: Any?) = x + y

fun box(): String {
    val t1 = stringPlus("a", "b")
    if (t1 != "ab") return "Failed: t1=$t1"

    val t2 = stringPlus("a", null)
    if (t2 != "anull") return "Failed: t2=$t2"

    val t3 = stringPlus(null, "b")
    if (t3 != "nullb") return "Failed: t3=$t3"

    val t4 = stringPlus(null, null)
    if (t4 != "nullnull") return "Failed: t4=$t4"

    return "OK"
}
