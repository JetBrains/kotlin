// ISSUE: KT-43710

// IGNORE_BACKEND: ANY
// IGNORE_IR_DESERIALIZATION_TEST: NATIVE
// REASON: red code (see corresponding diagnostic test)
// IGNORE_IR_DESERIALIZATION_TEST: JS_IR
// ^^^ Source code is not compiled in JS.

fun box(): String {
    build {
        letForTypeVariable { it.extensionProduceConcreteType() }
        setTypeVariable(TargetType())
    }
    return "OK"
}




class ConcreteType
class TargetType
class DifferentType

fun TargetType.extensionProduceConcreteType(): ConcreteType = ConcreteType()
fun DifferentType.extensionProduceConcreteType(): ConcreteType = ConcreteType()

class Buildee<TV> {
    fun setTypeVariable(value: TV) { storage = value }
    fun letForTypeVariable(action: (TV) -> ConcreteType): ConcreteType = storage.let(action)
    private var storage: TV = TargetType() as TV
}

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return Buildee<PTV>().apply(instructions)
}
