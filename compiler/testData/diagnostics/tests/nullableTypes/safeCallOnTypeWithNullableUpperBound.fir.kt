// !WITH_NEW_INFERENCE
fun <T> test(t: T): String? {
    if (t != null) {
        return t?.toString()
    }
    return t?.toString()
}

fun <T> T.testThis(): String? {
    if (this != null) {
        return this?.toString()
    }
    return this?.toString()
}