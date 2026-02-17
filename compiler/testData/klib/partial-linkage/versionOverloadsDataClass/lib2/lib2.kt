fun computeC(): String {
    val s = C(10).toString()
    return if (s == "C(a=10, b=B, b1=B1, c=42)") "OK" else "FAIL"
}

fun computeD(): String {
    val s = D(20).toString()
    return if (s == "D(a=20, a1=A1, b=B2, c=24)") "OK" else "FAIL"
}
