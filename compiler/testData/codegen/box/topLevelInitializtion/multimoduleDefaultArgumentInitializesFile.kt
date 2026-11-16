// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE: JS,Wasm-JS:2.4
// KT-89487: Fixed in 2.5.20-Beta1
// MODULE: state
// FILE: state.kt
var log = ""
var initializationCount = 0

fun defaultValue(): Unit {
    log += "default;"
}

// MODULE: lib(state)
// FILE: target.kt
private val marker = run {
    initializationCount++
    log += "init;"
}

fun target(unused: Unit = defaultValue()) {
    log += "body;"
}

// MODULE: main(state, lib)
// FILE: main.kt
fun box(): String {
    if (initializationCount != 0) return "initialized before call"

    target()
    if (log != "init;default;body;") return log
    if (initializationCount != 1) return "initialized more than once: $initializationCount"

    target(Unit)
    if (log != "init;default;body;body;") return log
    if (initializationCount != 1) return "reinitialized: $initializationCount"

    return "OK"
}
