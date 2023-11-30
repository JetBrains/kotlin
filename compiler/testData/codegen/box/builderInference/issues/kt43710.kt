// ISSUE: KT-43710

// IGNORE_LIGHT_ANALYSIS
// IGNORE_BACKEND: ANY
// REASON: red code (see corresponding diagnostic test)

fun box(): String {
    build {
        setTypeVariable(TargetType())
        letForTypeVariable { it.extensionProduceConcreteType() }
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
