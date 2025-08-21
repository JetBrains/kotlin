// JVM_ABI_K1_K2_DIFF: K2 serializes annotation parameter default values (KT-59526).

// MODULE: lib1
// FILE: lib1.kt

annotation class MyConfig(
    vararg val profiles: String = [],
)

// MODULE: box(lib1)
// FILE: box.kt

@MyConfig
fun box() = "OK"
