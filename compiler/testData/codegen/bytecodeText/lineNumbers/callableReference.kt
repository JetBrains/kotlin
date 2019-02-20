// IGNORE_BACKEND: JVM_IR

fun function() {
}

val property: Long
    get() = System.currentTimeMillis()

fun main() {
    ::function
    ::property
}

// There's one line number in `function`, one in `getProperty` and three in `main`.
// It's important that there are no additional line numbers in synthetic methods in anonymous classes for callable references
// 5 LINENUMBER
