// ISSUE: KT-56949

// IGNORE_LIGHT_ANALYSIS
// IGNORE_BACKEND_K1: ANY
// REASON: red code (see corresponding diagnostic test)

// IGNORE_BACKEND_K2: JVM_IR, WASM
// REASON: run-time failure (java.lang.ClassCastException: TargetType cannot be cast to DifferentType @ Kt56949Kt$box$1.invoke)

fun box(): String {
    build {
        setTypeVariable(TargetType())
        consumeDifferentTypeSubtype(getTypeVariable())
    }
    return "OK"
}




class TargetType
class DifferentType

fun <T: DifferentType> consumeDifferentTypeSubtype(value: T) {}

class Buildee<TV> {
    fun setTypeVariable(value: TV) { storage = value }
    fun getTypeVariable(): TV = storage
    private var storage: TV = TargetType() as TV
}

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return Buildee<PTV>().apply(instructions)
}
