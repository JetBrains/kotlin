// ISSUE: KT-53109

// IGNORE_BACKEND: ANDROID

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
