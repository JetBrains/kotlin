fun f(x: Any?): String {
    if (x is Array<String>) {
        for (i in x) {
            return i
        }
    }
    return "FAIL"
}

fun box(): String = f(Array<String>(1, {"OK"}))
