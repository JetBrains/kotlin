// ISSUE: KT-53553

// IGNORE_LIGHT_ANALYSIS
// IGNORE_BACKEND_K1: ANY
// REASON: red code (see corresponding diagnostic test)

// IGNORE_BACKEND_K2: JVM_IR, WASM
// REASON: run-time failure (java.lang.ClassCastException: TargetType cannot be cast to TargetTypeDerived @ Kt53553Kt$box$2.invoke)

fun box(): String {
    parallelBuild(
        {
            consumeTargetType(it)
        },
        {
            consumeTargetTypeDerived(it)
        }
    )
    return "OK"
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
    val value = TargetType() as PTV
    return Buildee<PTV>().apply {
        instructionsA(value)
        instructionsB(value)
    }
}
