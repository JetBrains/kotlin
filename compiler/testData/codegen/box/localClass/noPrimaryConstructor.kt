// KT-66105: SyntaxError: Identifier 'box' has already been declared
// IGNORE_BACKEND: WASM_JS, WASM_WASI
// WITH_STDLIB

import kotlin.test.*

fun box(s: String): String {
    class Local {
        constructor(x: Int) {
            this.x = x
        }

        constructor(z: String) {
            x = z.length
        }

        val x: Int

        fun result() = s
    }

    return Local(42).result() + Local("zzz").result()
}

fun box(): String {
    assertEquals("OKOK", box("OK"))
    return "OK"
}
