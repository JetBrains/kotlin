// ISSUE: KT-53109

// IGNORE_LIGHT_ANALYSIS
// IGNORE_BACKEND_K1: ANY
// REASON: red code

fun box(): String {
    build {
        typeVariableConsumer = { consumeTargetType(it) }
    }
    return "OK"
}




class TargetType

fun consumeTargetType(value: TargetType) {}

class Buildee<TV> {
    var typeVariableConsumer: (TV) -> Unit = { storage = it }
    private var storage: TV = TargetType() as TV
}

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return Buildee<PTV>().apply(instructions)
}
