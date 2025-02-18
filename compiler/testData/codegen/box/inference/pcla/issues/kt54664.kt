// ISSUE: KT-54664

// IGNORE_BACKEND: ANY
// IGNORE_IR_DESERIALIZATION_TEST: NATIVE
// REASON: red code (see corresponding diagnostic test)
// IGNORE_IR_DESERIALIZATION_TEST: JS_IR
// ^^^ Source code is not compiled in JS.

fun box(): String {
    build {
        setTypeVariable(TargetType())
        consumeDifferentTypeCallable(this::getTypeVariable)
    }
    return "OK"
}




class TargetType
class DifferentType

fun consumeDifferentType(value: DifferentType) {}
fun consumeDifferentTypeCallable(callable: () -> DifferentType) { consumeDifferentType(callable()) }

class Buildee<TV> {
    fun setTypeVariable(value: TV) { storage = value }
    fun getTypeVariable(): TV = storage
    private var storage: TV = TargetType() as TV
}

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return Buildee<PTV>().apply(instructions)
}
