// ISSUE: KT-60447
// CHECK_TYPE_WITH_EXACT

fun test() {
    val buildee = build {
        setTypeVariableProducerFunction { TargetType() }
        setTypeVariableConsumerFunction { it.<!UNRESOLVED_REFERENCE!>consumeConcreteType<!>(ConcreteType()) }
    }
    // exact type equality check — turns unexpected compile-time behavior into red code
    // considered to be non-user-reproducible code for the purposes of these tests
    checkExactType<Buildee<TargetType>>(buildee)
}




class ConcreteType
class TargetType {
    fun consumeConcreteType(value: ConcreteType) {}
}

class Buildee<TV> {
    var typeVariableConsumer: (TV) -> Unit = {}
    var typeVariableProducer: () -> TV = { null!! }
    fun setTypeVariableConsumerFunction(consumer: (TV) -> Unit) { typeVariableConsumer = consumer }
    fun setTypeVariableProducerFunction(producer: () -> TV) { typeVariableProducer = producer }
}

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return Buildee<PTV>().apply(instructions)
}
