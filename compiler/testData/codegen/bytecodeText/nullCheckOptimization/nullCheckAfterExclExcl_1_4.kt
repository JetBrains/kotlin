// !API_VERSION: LATEST
// IGNORE_BACKEND: JVM_IR

fun test(s: String?): Int {
    s!!
    if (s == null) {
        return 5
    }
    return 3
}

// 1 checkNotNull
// 0 IFNULL
// 0 IFNONNULL
