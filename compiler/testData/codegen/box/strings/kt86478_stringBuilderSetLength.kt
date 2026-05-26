// WITH_STDLIB

fun box(): String {
    val sb = StringBuilder("abc")

    sb.setLength(3)
    val result1 = sb.toString()
    if (result1 != "abc") return "Fail setLength(3): <$result1>, length=${result1.length}"

    sb.setLength(5)
    val result2 = sb.toString()
    if (result2 != "abc\u0000\u0000") return "Fail setLength(5): <$result2>, length=${result2.length}"

    sb.setLength(2)
    val result3 = sb.toString()
    if (result3 != "ab") return "Fail setLength(2): <$result3>, length=${result3.length}"

    sb.setLength(3)
    val result4 = sb.toString()
    if (result4 != "ab\u0000") return "Fail setLength(3) after setLength(2): <$result4>, length=${result4.length}"

    sb.append("cd")
    sb.setLength(4)

    val result5 = sb.toString()
    if (result5 != "ab\u0000c") return "Fail setLength(4) after append(\"cd\"): <$result5>, length=${result5.length}"

    sb.setLength(0)
    val result6 = sb.toString()
    if (result6 != "") return "Fail setLength(0): <$result6>, length=${result6.length}"

    return "OK"
}
