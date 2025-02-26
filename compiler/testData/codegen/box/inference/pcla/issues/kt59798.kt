// ISSUE: KT-59798
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD_2_0
// ^^^ Compiler v2.0.0: error: the type of a receiver hasn't been inferred yet. Please specify type argument for generic parameter 'PTV' of 'buildFromValue' explicitly

// IGNORE_BACKEND_K1: ANY
// REASON: red code (see corresponding diagnostic test)

fun box(): String {
    build {
        setTypeVariable(TargetType())
        getTypeVariable().let {}
    }
    return "OK"
}




class TargetType

class Buildee<TV> {
    fun setTypeVariable(value: TV) { storage = value }
    fun getTypeVariable(): TV = storage
    private var storage: TV = TargetType() as TV
}

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return Buildee<PTV>().apply(instructions)
}
