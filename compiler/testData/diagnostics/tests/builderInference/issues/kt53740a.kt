// ISSUE: KT-53740
// CHECK_TYPE_WITH_EXACT

fun test() {
    val buildee = parallelBuild(
        <!BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION!>{
            setTypeVariable(TargetType())
        }<!>,
        <!BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION!>{
            consumeDifferentType(getTypeVariable())
        }<!>
    )
    // exact type equality check — turns unexpected compile-time behavior into red code
    // considered to be non-user-reproducible code for the purposes of these tests
    checkExactType<Buildee<TargetType>>(<!TYPE_MISMATCH("Buildee<TargetType>; Buildee<Any>"), TYPE_MISMATCH("Buildee<Any>; Buildee<TargetType>")!>buildee<!>)
}




class TargetType
class DifferentType

fun consumeDifferentType(value: DifferentType) {}

class Buildee<TV> {
    fun setTypeVariable(value: TV) { storage = value }
    fun getTypeVariable(): TV = storage
    private var storage: TV = null!!
}

fun <PTV> parallelBuild(
    instructionsA: Buildee<PTV>.(PTV) -> Unit,
    instructionsB: Buildee<PTV>.(PTV) -> Unit
): Buildee<PTV> {
    return null!!
}
