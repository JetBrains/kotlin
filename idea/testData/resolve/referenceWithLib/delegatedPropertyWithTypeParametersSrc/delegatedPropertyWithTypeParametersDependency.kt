package dependency

fun <T, R> T.get(thisRef: R, desc: PropertyMetadata): Int {
    return 3
}

fun <T, R> T.set(thisRef: R, desc: PropertyMetadata, value: Int) {
}
