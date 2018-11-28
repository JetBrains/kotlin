// IGNORE_BACKEND: JVM_IR
fun foo(): String {
    fun bar(x: String, y: String = x): String {
        return y
    }

    return bar("OK")
}

fun box(): String {
    return foo()
}