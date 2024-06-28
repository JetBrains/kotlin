// ISSUE: KT-49160
// CHECK_TYPE_WITH_EXACT

fun test() {
    val outerBuildee = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>build<!> outerBuild@ {
        object {
            fun anonymousObjectMember() {
                val innerBuildee = build innerBuild@ {
                    this@outerBuild.setTypeVariable(TargetType())
                    this@innerBuild.setTypeVariable(TargetType())
                }
                // exact type equality check — turns unexpected compile-time behavior into red code
                // considered to be non-user-reproducible code for the purposes of these tests
                checkExactType<Buildee<TargetType>>(innerBuildee)
            }
        }
    }
    // exact type equality check — turns unexpected compile-time behavior into red code
    // considered to be non-user-reproducible code for the purposes of these tests
    checkExactType<Buildee<TargetType>>(<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>outerBuildee<!>)
}




class TargetType

class Buildee<TV> {
    fun setTypeVariable(value: TV) { storage = value }
    private var storage: TV = null!!
}

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return Buildee<PTV>().apply(instructions)
}
