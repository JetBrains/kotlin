// ISSUE: KT-55056

// IGNORE_LIGHT_ANALYSIS
// IGNORE_BACKEND: ANY
// REASON: red code (see corresponding diagnostic test)

fun box(): String {
    build {
        setTypeVariable(TargetType())
        consumeDifferentType(extensionReplaceOutProjectedTypeVariable(DifferentType()))
    }
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

fun <ETV> Buildee<out ETV>.extensionReplaceOutProjectedTypeVariable(value: ETV): ETV {
    this as Buildee<ETV>
    val temp = getTypeVariable()
    setTypeVariable(value)
    return temp
}

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return Buildee<PTV>().apply(instructions)
}
