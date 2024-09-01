// IGNORE_BACKEND: NATIVE
// IGNORE_BACKEND: WASM
// IGNORE_BACKEND: JS, JS_IR, JS_IR_ES6
// IGNORE_LIGHT_ANALYSIS

// CHECK_TYPE_WITH_EXACT

import kotlin.IllegalStateException

class Buildee<CT> {
    fun yield(arg: CT) {}
}

fun <FT> build(
    instructions: Buildee<FT>.() -> Unit
): Buildee<FT> {
    return Buildee<FT>().apply(instructions)
}

fun testYield() {
    val buildee = build {
        try {
            yield(throw IllegalStateException())
        } catch (e: IllegalStateException) {}
    }
    checkExactType<Buildee<Nothing>>(buildee)
}

fun box(): String {
    testYield()
    return "OK"
}
