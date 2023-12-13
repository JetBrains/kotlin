// ISSUE: KT-61310

// IGNORE_LIGHT_ANALYSIS
// REASON: red code (see corresponding diagnostic test)

fun box(): String {
    build {
        if (true) {
            setTypeVariable(GenericBox())
        } else {
            setTypeVariable(GenericBox<TargetType>())
        }
    }
    return "OK"
}




class TargetType
class GenericBox<T>

class Buildee<TV> {
    fun setTypeVariable(value: TV) { storage = value }
    private var storage: TV = GenericBox<TargetType>() as TV
}

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return Buildee<PTV>().apply(instructions)
}
