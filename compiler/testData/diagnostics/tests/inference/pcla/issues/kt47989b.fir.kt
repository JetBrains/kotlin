// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-47989
// CHECK_TYPE_WITH_EXACT

fun test() {
    val buildee = <!CANNOT_INFER_PARAMETER_TYPE!>build<!> {
        object: TypeSourceInterface {
            override fun <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>produceTargetTypeBuildee<!>() = this@build
        }
    }
    // exact type equality check — turns unexpected compile-time behavior into red code
    // considered to be non-user-reproducible code for the purposes of these tests
    checkExactType<<!CANNOT_INFER_PARAMETER_TYPE!>Buildee<TargetType><!>>(buildee)
}




class TargetType

interface TypeSourceInterface {
    fun produceTargetTypeBuildee(): Buildee<TargetType>
}

class Buildee<TV>

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return Buildee<PTV>().apply(instructions)
}
