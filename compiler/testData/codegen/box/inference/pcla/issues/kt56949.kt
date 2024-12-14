// ISSUE: KT-56949

// IGNORE_LIGHT_ANALYSIS
// IGNORE_BACKEND: ANY
// IGNORE_IR_DESERIALIZATION_TEST: NATIVE
// REASON: red code (see corresponding diagnostic test)
// IGNORE_IR_DESERIALIZATION_TEST: JS_IR
// ^^^ Source code is not compiled in JS.

fun box(): String {
    build {
        setTypeVariable(TargetType())
        consumeDifferentTypeSubtype(getTypeVariable())
    }
    return "OK"
}




class TargetType
class DifferentType

fun <T: DifferentType> consumeDifferentTypeSubtype(value: T) {}

class Buildee<TV> {
    fun setTypeVariable(value: TV) { storage = value }
    fun getTypeVariable(): TV = storage
    private var storage: TV = TargetType() as TV
}

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return Buildee<PTV>().apply(instructions)
}
