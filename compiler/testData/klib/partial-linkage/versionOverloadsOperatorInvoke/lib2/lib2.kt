fun testInvoke(): String {
    val r = 10()
    return if (r == 36) "OK" else "FAIL"
}