// ISSUE: KT-65300

// IGNORE_BACKEND_K2: ANY
// REASON: red code (see corresponding diagnostic test)

fun box(): String {
    build {
        fun typeInfoSourceFunction(typeInfoSourceParameter: Buildee<TargetType> = this) {}
    }
    return "OK"
}




class TargetType

class Buildee<TV>

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return Buildee<PTV>().apply(instructions)
}
