// ISSUE: KT-63840

// IGNORE_LIGHT_ANALYSIS
// IGNORE_BACKEND_K1: ANY
// REASON: red code (see corresponding diagnostic test)

// IGNORE_BACKEND_K2: JVM_IR
// REASON: run-time failure (java.lang.ClassCastException: TargetType cannot be cast to DifferentType @ Kt63840cKt$box$1.invoke)

fun box(): String {
    build {
        when {
            true -> replaceTypeVariable(TargetType())
            else -> DifferentType()
        }
    }
    return "OK"
}




class TargetType
class DifferentType

class Buildee<TV> {
    fun replaceTypeVariable(value: TV): TV { val temp = storage; storage = value; return temp }
    private var storage: TV = TargetType() as TV
}

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return Buildee<PTV>().apply(instructions)
}
