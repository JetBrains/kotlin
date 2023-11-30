// ISSUE: KT-50453

// IGNORE_LIGHT_ANALYSIS
// IGNORE_BACKEND_K1: ANY
// REASON: red code (see corresponding diagnostic test)

// IGNORE_BACKEND_K2: JVM_IR, WASM
// REASON: run-time failure (java.lang.ClassCastException: TargetType cannot be cast to DifferentType @ Kt50453Kt$box$1.invoke)

fun box(): String {
    build {
        setTypeVariable(TargetType())
        consume(getTypeVariable())
    }
    return "OK"
}




class TargetType
class DifferentType

fun consume(value: Any) {}
fun consume(value: DifferentType) {}

class Buildee<TV> {
    fun setTypeVariable(value: TV) { storage = value }
    fun getTypeVariable(): TV = storage
    private var storage: TV = TargetType() as TV
}

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return Buildee<PTV>().apply(instructions)
}
