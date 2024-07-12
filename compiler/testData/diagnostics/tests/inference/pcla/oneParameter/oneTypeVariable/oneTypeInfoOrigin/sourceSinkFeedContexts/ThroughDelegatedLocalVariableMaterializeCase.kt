// FIR_IDENTICAL
// DIAGNOSTICS: -UNCHECKED_CAST

// CHECK_TYPE_WITH_EXACT

// ISSUE: KT-60274
// (also see an analogous codegen test)

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
