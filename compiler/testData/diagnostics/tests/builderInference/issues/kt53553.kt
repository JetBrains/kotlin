// ISSUE: KT-53553
// CHECK_TYPE_WITH_EXACT

fun test() {
    val buildee = parallelBuild(
        <!BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION!>{
            consumeTargetType(it)
        }<!>,
        <!BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION!>{
            consumeTargetTypeDerived(it)
        }<!>
    )
    // exact type equality check — turns unexpected compile-time behavior into red code
    // considered to be non-user-reproducible code for the purposes of these tests
    checkExactType<Buildee<TargetType>>(buildee)
}




open class TargetType
class TargetTypeDerived: TargetType()

fun consumeTargetType(value: TargetType) {}

fun consumeTargetTypeDerived(value: TargetTypeDerived) {}

class Buildee<TV>

fun <PTV> parallelBuild(
    instructionsA: Buildee<PTV>.(PTV) -> Unit,
    instructionsB: Buildee<PTV>.(PTV) -> Unit
): Buildee<PTV> {
    return null!!
}
