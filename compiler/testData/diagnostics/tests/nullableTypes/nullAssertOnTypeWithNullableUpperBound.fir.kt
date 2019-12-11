// !WITH_NEW_INFERENCE
fun <T> test(t: T): T {
    if (t != null) {
        return t!!
    }
    return t!!
}

fun <T> T.testThis(): String {
    if (this != null) {
        return this!!.toString()
    }
    return this!!.toString()
}

