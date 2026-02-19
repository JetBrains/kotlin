fun test() {
    val value = System.getProperty("key")
    if (value != null) {
        value.uppercase()
    }
}

// 1 IFNULL
// 0 IFNONNULL
