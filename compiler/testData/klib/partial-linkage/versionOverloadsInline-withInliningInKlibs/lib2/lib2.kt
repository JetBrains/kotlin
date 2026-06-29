fun computeInlineIntroduced(): String {
    val r = inlineFun { 7 }
    return if (r == 7) "OK" else "FAIL"
}
