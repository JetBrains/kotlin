// ISSUE: KT-57707

// IGNORE_LIGHT_ANALYSIS
// IGNORE_BACKEND_K1: ANY
// REASON: red code (see corresponding diagnostic test)

fun box(): String {
    build {
        setTypeVariable(TargetType())
        extensionSetOutProjectedTypeVariable(DifferentType())
    }
    return "OK"
}




class TargetType
class DifferentType

class Buildee<TV> {
    fun setTypeVariable(value: TV) { storage = value }
    private var storage: TV = TargetType() as TV
}

fun <ETV> Buildee<out ETV>.extensionSetOutProjectedTypeVariable(value: ETV) {
    this as Buildee<ETV>
    setTypeVariable(value)
}

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return Buildee<PTV>().apply(instructions)
}
