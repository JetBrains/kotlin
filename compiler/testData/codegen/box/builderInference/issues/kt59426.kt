// ISSUE: KT-59426

// IGNORE_LIGHT_ANALYSIS
// IGNORE_BACKEND_K1: ANY
// REASON: red code (see corresponding diagnostic test)

// IGNORE_BACKEND_K2: JVM_IR, WASM
// REASON: run-time failure (java.lang.ClassCastException: DifferentType cannot be cast to TargetType @ Kt59426Kt.consumeBuildeeReceiver)

fun box(): String {
    build {
        setTypeVariable(DifferentType())
        consumeBuildeeReceiver()
    }
    return "OK"
}




class TargetType
class DifferentType

fun consumeTargetType(value: TargetType) {}

class Buildee<TV> {
    fun setTypeVariable(value: TV) { storage = value }
    fun getTypeVariable(): TV = storage
    private var storage: TV = TargetType() as TV
}

fun Buildee<TargetType>.consumeBuildeeReceiver() {
    consumeTargetType(getTypeVariable())
}

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return Buildee<PTV>().apply(instructions)
}
