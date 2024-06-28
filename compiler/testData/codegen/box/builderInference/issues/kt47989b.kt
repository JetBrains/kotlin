// ISSUE: KT-47989

// IGNORE_LIGHT_ANALYSIS
// IGNORE_BACKEND: ANY
// REASON: red code (see corresponding diagnostic test)

fun box(): String {
    build {
        object: TypeSourceInterface {
            override fun produceTargetTypeBuildee() = this@build
        }
    }
    return "OK"
}




class TargetType

interface TypeSourceInterface {
    fun produceTargetTypeBuildee(): Buildee<TargetType>
}

class Buildee<TV>

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return Buildee<PTV>().apply(instructions)
}
