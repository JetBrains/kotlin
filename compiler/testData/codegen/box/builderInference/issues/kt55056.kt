// ISSUE: KT-55056

// IGNORE_LIGHT_ANALYSIS
// IGNORE_BACKEND_K1: ANY
// REASON: red code (see corresponding diagnostic test)

// IGNORE_BACKEND_K2: JVM_IR, WASM
// REASON: run-time failure (java.lang.ClassCastException: TargetType cannot be cast to DifferentType @ Kt55056Kt$box$1.invoke)

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
