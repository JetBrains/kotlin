// ISSUE: KT-49160
// CHECK_TYPE_WITH_EXACT

fun test() {
    val outerBuildee = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>build<!> outerBuild@ {
        val innerBuildee = build innerBuild@ {
            this@outerBuild.setTypeVariable(TargetType())
            this@innerBuild.setTypeVariable(TargetType())
        }
        // exact type equality check — turns unexpected compile-time behavior into red code
        // considered to be non-user-reproducible code for the purposes of these tests
        checkExactType<Buildee<TargetType>>(innerBuildee)
    }
    // exact type equality check — turns unexpected compile-time behavior into red code
    // considered to be non-user-reproducible code for the purposes of these tests
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>checkExactType<!><<!CANNOT_INFER_PARAMETER_TYPE!>Buildee<TargetType><!>>(outerBuildee)
}




class TargetType

class Buildee<TV> {
    fun setTypeVariable(value: TV) { storage = value }
    private var storage: TV = null!!
}

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return Buildee<PTV>().apply(instructions)
}
