// ISSUE: KT-59426
// CHECK_TYPE_WITH_EXACT

fun test() {
    val buildee = <!NEW_INFERENCE_ERROR!>build {
        setTypeVariable(DifferentType())
        <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>consumeBuildeeReceiver<!>()
    }<!>
    // exact type equality check — turns unexpected compile-time behavior into red code
    // considered to be non-user-reproducible code for the purposes of these tests
    checkExactType<Buildee<TargetType>>(buildee)
}




class TargetType
class DifferentType

fun consumeTargetType(value: TargetType) {}

class Buildee<TV> {
    fun setTypeVariable(value: TV) { storage = value }
    fun getTypeVariable(): TV = storage
    private var storage: TV = null!!
}

fun Buildee<TargetType>.consumeBuildeeReceiver() {
    consumeTargetType(getTypeVariable())
}

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return Buildee<PTV>().apply(instructions)
}
