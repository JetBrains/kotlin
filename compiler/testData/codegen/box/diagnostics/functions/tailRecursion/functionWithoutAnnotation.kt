// IGNORE_BACKEND_WITHOUT_CHECK: JS

fun withoutAnnotation(x : Int) : Int {
    if (x > 0) {
        return 1 + withoutAnnotation(x - 1)
    }
    return 0
}

fun box(): String {
    val r = withoutAnnotation(10)
    if (r == 10) return "OK"
    return "Fail $r"
}