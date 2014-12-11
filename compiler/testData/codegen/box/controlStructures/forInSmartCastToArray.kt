fun f(x: Any?): String {
    if (x is Array<String>) {
        for (i in x) {
            return i
        }
    }
    return "FAIL"
}

fun box(): String {
    val a = arrayOfNulls<String>(1) as Array<String>
    a[0] = "OK"
    return f(a)
}
