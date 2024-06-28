// TARGET_BACKEND: JVM_IR
// FILE: 1.kt

@OptIn(ExperimentalStdlibApi::class)
fun test() {
    E.entries
}

// FILE: 2.kt

enum class E

// 0 EntriesMappings
