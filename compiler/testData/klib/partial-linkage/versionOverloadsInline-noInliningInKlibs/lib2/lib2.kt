fun computeInlineIntroduced(): String {
    val r = inlineFun { 7 }
    return if (r == 130) "OK" else "FAIL"
}