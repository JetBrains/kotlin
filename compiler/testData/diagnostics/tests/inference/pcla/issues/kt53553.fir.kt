// ISSUE: KT-53553
// CHECK_TYPE_WITH_EXACT

fun test() {
    val buildee = parallelBuild(
        {
            consumeTargetTypeBase(it)
        },
        {
            consumeTargetType(it)
        }
    )
    // exact type equality check — turns unexpected compile-time behavior into red code
    // considered to be non-user-reproducible code for the purposes of these tests
    checkExactType<Buildee<TargetType>>(buildee)
}




open class TargetTypeBase
class TargetType: TargetTypeBase()

fun consumeTargetTypeBase(value: TargetTypeBase) {}

fun consumeTargetType(value: TargetType) {}

class Buildee<TV>

fun <PTV> parallelBuild(
    instructionsA: Buildee<PTV>.(PTV) -> Unit,
    instructionsB: Buildee<PTV>.(PTV) -> Unit
): Buildee<PTV> {
    return null!!
}
