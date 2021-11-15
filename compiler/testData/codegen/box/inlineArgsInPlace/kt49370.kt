// !OPT_IN: kotlin.ExperimentalStdlibApi
// WITH_STDLIB
// IGNORE_BACKEND: WASM

fun box(): String {
    1L.mod("123a".indexOfAny("a".toCharArray()))
    return "OK"
}
