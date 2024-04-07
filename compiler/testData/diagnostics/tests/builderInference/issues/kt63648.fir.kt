// ISSUE: KT-63648
// CHECK_TYPE_WITH_EXACT

fun test() {
    val buildee = build {
        setTypeVariable(TargetType())
        <!ARGUMENT_TYPE_MISMATCH("TargetType; DifferentType")!>getTypeVariable().<!UNRESOLVED_REFERENCE_WRONG_RECEIVER("fun DifferentType.consumeDifferentTypeReceiver(): Unit")!>consumeDifferentTypeReceiver<!>()<!>
    }
    // exact type equality check — turns unexpected compile-time behavior into red code
    // considered to be non-user-reproducible code for the purposes of these tests
    checkExactType<Buildee<TargetType>>(buildee)
}




class TargetType
class DifferentType

fun DifferentType.consumeDifferentTypeReceiver() {}

class Buildee<TV> {
    fun setTypeVariable(value: TV) { storage = value }
    fun getTypeVariable(): TV = storage
    private var storage: TV = null!!
}

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return Buildee<PTV>().apply(instructions)
}
