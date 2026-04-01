fun computeInner(): String {
    val c = C()
    val a = c.A(10)
    val s = a.foo()
    return if (s == "a=10,b=B,b1=B1,c=11") "OK" else "FAIL"
}