// ISSUE: KT-55056

// IGNORE_BACKEND: ANY
// IGNORE_IR_DESERIALIZATION_TEST: NATIVE
// REASON: red code (see corresponding diagnostic test)
// IGNORE_IR_DESERIALIZATION_TEST: JS_IR
// ^^^ Source code is not compiled in JS.

fun box(): String {
    build {
        setTypeVariable(TargetType())
        consumeDifferentType(extensionReplaceOutProjectedTypeVariable(DifferentType()))
    }
    return "OK"
}




class TargetType
class DifferentType

fun consumeDifferentType(value: DifferentType) {}

class Buildee<TV> {
    fun setTypeVariable(value: TV) { storage = value }
    fun getTypeVariable(): TV = storage
    private var storage: TV = TargetType() as TV
}

fun <ETV> Buildee<out ETV>.extensionReplaceOutProjectedTypeVariable(value: ETV): ETV {
    this as Buildee<ETV>
    val temp = getTypeVariable()
    setTypeVariable(value)
    return temp
}

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return Buildee<PTV>().apply(instructions)
}
