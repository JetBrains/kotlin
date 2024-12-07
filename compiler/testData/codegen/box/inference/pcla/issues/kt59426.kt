// ISSUE: KT-59426

// IGNORE_LIGHT_ANALYSIS
// IGNORE_BACKEND: ANY
// REASON: red code (see corresponding diagnostic test)

fun box(): String {
    build {
        setTypeVariable(DifferentType())
        consumeBuildeeReceiver()
    }
    return "OK"
}




class TargetType
class DifferentType

fun consumeTargetType(value: TargetType) {}

class Buildee<TV> {
    fun setTypeVariable(value: TV) { storage = value }
    fun getTypeVariable(): TV = storage
    private var storage: TV = TargetType() as TV
}

fun Buildee<TargetType>.consumeBuildeeReceiver() {
    consumeTargetType(getTypeVariable())
}

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return Buildee<PTV>().apply(instructions)
}
