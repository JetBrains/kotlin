// ISSUE: KT-55281

// IGNORE_BACKEND_K2: ANY
// IGNORE_IR_DESERIALIZATION_TEST: NATIVE
// REASON: red code (see corresponding diagnostic test)
// IGNORE_IR_DESERIALIZATION_TEST: JS_IR
// ^^^ Source code is not compiled in JS.

fun box(): String {
    build {
        this as DerivedBuildee<*>
        consumeNullableAny(getTypeVariable())
    }
    return "OK"
}




class TargetType

fun consumeNullableAny(value: Any?) {}

open class Buildee<TV> {
    fun getTypeVariable(): TV = storage
    private var storage: TV = null as TV
}

class DerivedBuildee<TA>: Buildee<TA>()

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return DerivedBuildee<PTV>().apply(instructions)
}
