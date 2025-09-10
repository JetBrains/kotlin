// CHECK_TYPE_WITH_EXACT
// DISABLE_IR_VISIBILITY_CHECKS: ANY

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
