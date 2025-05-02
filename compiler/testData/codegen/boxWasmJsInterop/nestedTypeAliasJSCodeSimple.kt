// LANGUAGE: +NestedTypeAliases
// TARGET_BACKEND: WASM

external interface I {
    val x: Int
}

class AliasHolder {
    typealias TA = I
}

fun foo(): AliasHolder.TA = js("({ x: 123 })")

fun box(): String {
    val t = foo()
    return if (t.x == 123) "OK" else "FAIL"
}