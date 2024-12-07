// MODULE: lib1
// FILE: lib1.kt
object L1 {
    lateinit var block: () -> Unit
}
// MODULE: lib2(lib1)
// FILE: lib2.kt
object L2 {
    val sideEffect = L1.block()
    const val x = 42
    fun triggerInitialization() {}
}

// MODULE: main(lib1, lib2)
// FILE: main.kt
fun box(): String {
    var result = 0
    L1.block = { result = L2.x }
    L2.triggerInitialization()
    if (result != 42) return "FAIL: $result"
    return "OK"
}