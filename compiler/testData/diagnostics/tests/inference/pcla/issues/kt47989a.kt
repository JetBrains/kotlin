// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-47989
// CHECK_TYPE_WITH_EXACT

fun test() {
    val buildee = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>build<!> {
        object: TypeSourceInterface {
            override fun produceTargetType() = getTypeVariable()
        }
    }
    // exact type equality check — turns unexpected compile-time behavior into red code
    // considered to be non-user-reproducible code for the purposes of these tests
    checkExactType<Buildee<TargetType>>(<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>buildee<!>)
}




class TargetType

interface TypeSourceInterface {
    fun produceTargetType(): TargetType
}

class Buildee<TV> {
    fun getTypeVariable(): TV = storage
    private var storage: TV = null!!
}

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return Buildee<PTV>().apply(instructions)
}
