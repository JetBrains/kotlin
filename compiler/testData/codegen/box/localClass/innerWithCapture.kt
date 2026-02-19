// KT-66105: SyntaxError: Identifier 'box' has already been declared
// IGNORE_BACKEND: WASM_JS, WASM_WASI
// WITH_STDLIB

import kotlin.test.*

fun box(s: String): String {
    class Local {
        open inner class Inner() {
            open fun result() = s
        }
    }

    return Local().Inner().result()
}

fun box(): String {
    return box("OK")
}
