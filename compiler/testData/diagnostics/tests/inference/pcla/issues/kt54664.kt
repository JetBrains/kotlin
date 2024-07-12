// ISSUE: KT-54664
// CHECK_TYPE_WITH_EXACT

fun test() {
    val buildee = build {
        setTypeVariable(TargetType())
        consumeDifferentTypeCallable(<!TYPE_MISMATCH("DifferentType; TargetType"), TYPE_MISMATCH("() -> DifferentType; KFunction0<TargetType>")!>this::getTypeVariable<!>)
    }
    // exact type equality check — turns unexpected compile-time behavior into red code
    // considered to be non-user-reproducible code for the purposes of these tests
    checkExactType<Buildee<TargetType>>(buildee)
}




class TargetType
class DifferentType

fun consumeDifferentType(value: DifferentType) {}
fun consumeDifferentTypeCallable(callable: () -> DifferentType) { consumeDifferentType(callable()) }

class Buildee<TV> {
    fun setTypeVariable(value: TV) { storage = value }
    fun getTypeVariable(): TV = storage
    private var storage: TV = null!!
}

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return Buildee<PTV>().apply(instructions)
}
