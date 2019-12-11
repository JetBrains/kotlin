// !WITH_NEW_INFERENCE

fun box() : String {
    test {
        return@box "123"
    }

    return "OK"
}

inline fun <T> test(p: T) {
    p.toString()
}