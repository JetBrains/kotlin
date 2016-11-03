// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

fun box(): String {
    val f = "kotlin"::length
    val result = f.get()
    return if (result == 6) "OK" else "Fail: $result"
}
