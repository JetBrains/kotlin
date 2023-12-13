// ISSUE: KT-56949
// CHECK_TYPE_WITH_EXACT

fun test() {
    val buildee = build {
        setTypeVariable(TargetType())
        <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>consumeDifferentTypeSubtype<!>(<!ARGUMENT_TYPE_MISMATCH!>getTypeVariable()<!>)
    }
    // exact type equality check — turns unexpected compile-time behavior into red code
    // considered to be non-user-reproducible code for the purposes of these tests
    checkExactType<Buildee<TargetType>>(buildee)
}




class TargetType
class DifferentType

fun <T: <!FINAL_UPPER_BOUND!>DifferentType<!>> consumeDifferentTypeSubtype(value: T) {}

class Buildee<TV> {
    fun setTypeVariable(value: TV) { storage = value }
    fun getTypeVariable(): TV = storage
    private var storage: TV = null!!
}

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return Buildee<PTV>().apply(instructions)
}
