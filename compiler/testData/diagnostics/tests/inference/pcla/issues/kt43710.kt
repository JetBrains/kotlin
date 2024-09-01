// ISSUE: KT-43710
// CHECK_TYPE_WITH_EXACT

fun test() {
    val buildee = build {
        setTypeVariable(TargetType())
        letForTypeVariable <!TYPE_MISMATCH("ConcreteType; Unit")!>{ <!STUB_TYPE_IN_RECEIVER_CAUSES_AMBIGUITY!>it<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, OVERLOAD_RESOLUTION_AMBIGUITY, OVERLOAD_RESOLUTION_AMBIGUITY_BECAUSE_OF_STUB_TYPES!>extensionProduceConcreteType<!>() }<!>
    }
    // exact type equality check — turns unexpected compile-time behavior into red code
    // considered to be non-user-reproducible code for the purposes of these tests
    checkExactType<Buildee<TargetType>>(buildee)
}




class ConcreteType
class TargetType
class DifferentType

fun TargetType.extensionProduceConcreteType(): ConcreteType = ConcreteType()
fun DifferentType.extensionProduceConcreteType(): ConcreteType = ConcreteType()

class Buildee<TV> {
    fun setTypeVariable(value: TV) { storage = value }
    fun letForTypeVariable(action: (TV) -> ConcreteType): ConcreteType = storage.let(action)
    private var storage: TV = null!!
}

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return Buildee<PTV>().apply(instructions)
}
