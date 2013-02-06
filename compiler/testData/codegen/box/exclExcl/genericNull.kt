fun foo<T>(t: T) {
    t!!
}

fun box(): String {
    try {
        foo<Any?>(null)
    } catch (e: Exception) {
        return "OK"
    }
    return "Fail"
}
