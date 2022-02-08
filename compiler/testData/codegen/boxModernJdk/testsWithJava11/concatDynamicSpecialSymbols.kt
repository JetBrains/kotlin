
fun test(a: String, b: String?): String {
    return a + "\u0001" + 2.toChar() + 3.toChar() + 4L + b + 5.0 + 6F + '7' + b + "\u0002" + 1.toChar()
}

fun box(): String {
    val test = test("O", "K")
    return if (test != "O\u0001\u0002\u00034K5.06.07K\u0002\u0001") "fail  ${test}" else "OK"
}
