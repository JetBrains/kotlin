// IGNORE_BACKEND: JVM_IR
fun test() {
    val value = System.getProperty("key")
    if (value != null) {
        value.toUpperCase()
    }
}

// 1 IFNULL
// 0 IFNONNULL
