fun computeSecondary(): String {
    val s1 = C().foo()
    val s2 = C(true).foo()
    return if (s1 == "a=1,b=B,c=false" && s2 == "a=2,b=B,c=false") "OK" else "FAIL"
}