// !API_VERSION: LATEST
// IGNORE_BACKEND: JVM_IR

fun test(s: String?): Int {
    s!!
    s!!
    s!!
    s!!
    s!!
    return 0
}

// 1 checkNotNull
