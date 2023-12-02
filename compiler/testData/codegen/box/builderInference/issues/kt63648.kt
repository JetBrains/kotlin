// ISSUE: KT-63648

// IGNORE_LIGHT_ANALYSIS
// IGNORE_BACKEND: ANY
// REASON: red code (see corresponding diagnostic test)

fun box(): String {
    build {
        setTypeVariable(TargetType())
        getTypeVariable().consumeDifferentTypeReceiver()
    }
    return "OK"
}




class TargetType
class DifferentType

fun DifferentType.consumeDifferentTypeReceiver() {}

class Buildee<TV> {
    fun setTypeVariable(value: TV) { storage = value }
    fun getTypeVariable(): TV = storage
    private var storage: TV = TargetType() as TV
}

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return Buildee<PTV>().apply(instructions)
}
