fun testCallableRef(): String {
    val r1 = cref("abcd")
    val r2 = cref()
    return if (r1 == 4 && r2 == 1) "OK" else "FAIL"
}
