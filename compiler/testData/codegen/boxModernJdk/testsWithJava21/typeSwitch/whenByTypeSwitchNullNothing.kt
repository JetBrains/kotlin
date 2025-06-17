// TARGET_BACKEND: JVM
// WHEN_EXPRESSIONS: INDY


fun test(k: Any?): Int {
    return when (k) {
        is Nothing -> 1
        is String -> 3
        //is String? -> -3 // instanceOf here (because of ifnonnull)
        null -> -3
        //is String? -> 2 // indy here as nullcheck is higher
        else -> 4
    }
}

