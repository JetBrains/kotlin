// IGNORE_BACKEND: NATIVE
// IGNORE_BACKEND: WASM
// IGNORE_BACKEND: JS, JS_IR, JS_IR_ES6
// IGNORE_LIGHT_ANALYSIS

// CHECK_TYPE_WITH_EXACT

import kotlin.reflect.KProperty

class Buildee<CT> {
    fun yield(arg: CT) {}
}

fun <FT> build(
    instructions: Buildee<FT>.() -> Unit
): Buildee<FT> {
    return Buildee<FT>().apply(instructions)
}

class UserKlass

class Delegate<T>(private val value: T) {
    operator fun getValue(reference: Nothing?, property: KProperty<*>): T = value
}

fun testYield() {
    val arg: UserKlass = UserKlass()
    val buildee = build {
        val temp by Delegate(arg)
        yield(temp)
    }
    checkExactType<Buildee<UserKlass>>(buildee)
}

fun box(): String {
    testYield()
    return "OK"
}
