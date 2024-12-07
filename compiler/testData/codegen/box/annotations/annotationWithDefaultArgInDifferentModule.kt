// MODULE: lib1
// FILE: lib1.kt

annotation class MyConfig(
    vararg val profiles: String = [],
)

// MODULE: box(lib1)
// FILE: box.kt

@MyConfig
fun box() = "OK"
