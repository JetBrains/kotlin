// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

fun <T> foo(t: T) {
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
