// ISSUE: KT-49160

// IGNORE_LIGHT_ANALYSIS
// IGNORE_BACKEND_K1: ANY
// REASON: red code (see corresponding diagnostic test)

fun box(): String {
    build outerBuild@ {
        class LocalClass {
            fun localClassMember() {
                build innerBuild@ {
                    this@outerBuild.setTypeVariable(TargetType())
                    this@innerBuild.setTypeVariable(TargetType())
                }
            }
        }
    }
    return "OK"
}




class TargetType

class Buildee<TV> {
    fun setTypeVariable(value: TV) { storage = value }
    private var storage: TV = TargetType() as TV
}

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return Buildee<PTV>().apply(instructions)
}
