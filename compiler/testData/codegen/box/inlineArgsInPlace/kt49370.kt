// !OPT_IN: kotlin.ExperimentalStdlibApi
// WITH_RUNTIME
// IGNORE_BACKEND: WASM

fun box(): String {
    1L.mod("123a".indexOfAny("a".toCharArray()))
    return "OK"
}
