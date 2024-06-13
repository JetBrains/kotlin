// API_VERSION: LATEST_STABLE

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
