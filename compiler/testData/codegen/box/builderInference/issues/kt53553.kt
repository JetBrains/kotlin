// ISSUE: KT-53553

// IGNORE_LIGHT_ANALYSIS
// IGNORE_BACKEND_K1: ANY
// REASON: red code (see corresponding diagnostic test)

fun box(): String {
    val buildee = parallelBuild(
        {
            consumeTargetTypeBase(it)
        },
        {
            consumeTargetType(it)
        }
    )
    consumeTargetTypeBuildee(buildee)
    return "OK"
}




open class TargetTypeBase
class TargetType: TargetTypeBase()

fun consumeTargetTypeBase(value: TargetTypeBase) {}

fun consumeTargetType(value: TargetType) {}
fun consumeTargetTypeBuildee(value: Buildee<TargetType>) {}

class Buildee<TV>

fun <PTV> parallelBuild(
    instructionsA: Buildee<PTV>.(PTV) -> Unit,
    instructionsB: Buildee<PTV>.(PTV) -> Unit
): Buildee<PTV> {
    val value = TargetType() as PTV
    return Buildee<PTV>().apply {
        instructionsA(value)
        instructionsB(value)
    }
}
