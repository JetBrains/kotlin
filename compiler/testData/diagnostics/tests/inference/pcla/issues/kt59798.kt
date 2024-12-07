// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-59798
// CHECK_TYPE_WITH_EXACT

fun test() {
    val buildee = build {
        setTypeVariable(TargetType())
        <!BUILDER_INFERENCE_STUB_RECEIVER!>getTypeVariable()<!>.let {}
    }
    // exact type equality check — turns unexpected compile-time behavior into red code
    // considered to be non-user-reproducible code for the purposes of these tests
    checkExactType<Buildee<TargetType>>(buildee)
}




class TargetType

class Buildee<TV> {
    fun setTypeVariable(value: TV) { storage = value }
    fun getTypeVariable(): TV = storage
    private var storage: TV = null!!
}

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return Buildee<PTV>().apply(instructions)
}
