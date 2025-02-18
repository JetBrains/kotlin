// ISSUE: KT-53740

// IGNORE_BACKEND: ANY
// IGNORE_IR_DESERIALIZATION_TEST: NATIVE
// REASON: red code (see corresponding diagnostic test)
// IGNORE_IR_DESERIALIZATION_TEST: JS_IR
// ^^^ Source code is not compiled in JS.

fun box(): String {
    parallelInOutBuild(
        {
            setInProjectedTypeVariable(TargetType())
        },
        {
            consumeDifferentType(getOutProjectedTypeVariable())
        }
    )
    return "OK"
}




class TargetType
class DifferentType

fun consumeDifferentType(value: DifferentType) {}

class Buildee<TV> {
    fun setTypeVariable(value: TV) { storage = value }
    fun getTypeVariable(): TV = storage
    private var storage: TV = TargetType() as TV
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
    val value = TargetType() as PTV
    return Buildee<PTV>().apply {
        InBuildee(this).inProjectedInstructions(value)
        OutBuildee(this).outProjectedInstructions(value)
    }
}
