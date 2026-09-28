// TARGET_BACKEND: JVM
// FILE: 1.kt

enum class E

// FILE: 2.kt

@OptIn(ExperimentalStdlibApi::class)
fun test() {
    E.entries
}

// 0 EntriesMappings
