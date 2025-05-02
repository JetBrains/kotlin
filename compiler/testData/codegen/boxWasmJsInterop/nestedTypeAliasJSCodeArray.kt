// LANGUAGE: +NestedTypeAliases
// TARGET_BACKEND: WASM

external interface I : JsAny {
    val x: String
}

class AliasHolder {
    typealias JsArr = JsArray<I>
    typealias Arr   = Array<I>
}

fun fooJs(): AliasHolder.JsArr = js("""
    [ { x: '1' }, { x: '2' }, { x: '3' } ]
""")

fun foo(): AliasHolder.Arr = fooJs().toArray()

fun box(): String {
    val result = foo()
        .joinToString(",") { it.x }
    return if (result == "1,2,3") "OK" else "FAIL"
}