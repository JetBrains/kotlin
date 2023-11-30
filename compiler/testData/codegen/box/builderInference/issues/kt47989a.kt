// ISSUE: KT-47989

// IGNORE_LIGHT_ANALYSIS
// IGNORE_BACKEND: ANY
// REASON: red code (see corresponding diagnostic test)

fun box(): String {
    build {
        object: TypeSourceInterface {
            override fun produceTargetType() = getTypeVariable()
        }
    }
    return "OK"
}




class TargetType

interface TypeSourceInterface {
    fun produceTargetType(): TargetType
}

class Buildee<TV> {
    fun getTypeVariable(): TV = storage
    private var storage: TV = TargetType() as TV
}

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return Buildee<PTV>().apply(instructions)
}
