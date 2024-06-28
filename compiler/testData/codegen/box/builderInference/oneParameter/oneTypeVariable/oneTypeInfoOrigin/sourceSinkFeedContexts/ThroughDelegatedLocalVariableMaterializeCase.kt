// IGNORE_BACKEND_K2: NATIVE
// IGNORE_BACKEND_K2: WASM
// IGNORE_BACKEND_K2: JS, JS_IR, JS_IR_ES6
// IGNORE_LIGHT_ANALYSIS

// CHECK_TYPE_WITH_EXACT

// IGNORE_BACKEND_K1: ANY
// ISSUE: KT-60274

import kotlin.reflect.KProperty

class Buildee<CT> {
    fun materialize(): CT = UserKlass() as CT
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

fun testMaterialize() {
    fun consume(arg: UserKlass) {}
    val buildee = build {
        val temp by Delegate(materialize())
        consume(temp)
    }
    checkExactType<Buildee<UserKlass>>(buildee)
}

fun box(): String {
    testMaterialize()
    return "OK"
}
