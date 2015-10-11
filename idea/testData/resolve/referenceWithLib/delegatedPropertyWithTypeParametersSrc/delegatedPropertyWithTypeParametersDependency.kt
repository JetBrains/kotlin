package dependency

public fun <T, R> T.getValue(thisRef: R, desc: PropertyMetadata): Int {
    return 3
}

public fun <T, R> T.setValue(thisRef: R, desc: PropertyMetadata, value: Int) {
}
