fun computeCtorTypeParams(): String {
    val g = G("test")
    val s = g.foo()
    return if (s == "t=test,r=test") "OK" else "FAIL"
}