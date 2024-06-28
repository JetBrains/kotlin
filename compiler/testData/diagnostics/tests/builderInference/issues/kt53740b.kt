// ISSUE: KT-53740
// CHECK_TYPE_WITH_EXACT

fun test() {
    val buildee = parallelInOutBuild(
        <!BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION!>{
            setInProjectedTypeVariable(TargetType())
        }<!>,
        <!BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION!>{
            consumeDifferentType(getOutProjectedTypeVariable())
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

class OutBuildee<out OTV>(private val buildee: Buildee<out OTV>) {
    fun getOutProjectedTypeVariable(): OTV = buildee.getTypeVariable()
}

class InBuildee<in ITV>(private val buildee: Buildee<in ITV>) {
    fun setInProjectedTypeVariable(value: ITV) { buildee.setTypeVariable(value) }
}

fun <PTV> parallelInOutBuild(
    inProjectedInstructions: InBuildee<PTV>.(PTV) -> Unit,
    outProjectedInstructions: OutBuildee<PTV>.(PTV) -> Unit
): Buildee<PTV> {
    return null!!
}
