var result = "Fail"

fun foo() {
    try {
        return
    } finally {
        result = "OK"
    }
}

fun box(): String {
    foo()
    return result
}
