// ISSUE: KT-60447

// IGNORE_LIGHT_ANALYSIS
// IGNORE_BACKEND: ANY
// REASON: red code (see corresponding diagnostic test)

fun box(): String {
    val buildee = build {
        setTypeVariableProducerFunction { TargetType() }
        setTypeVariableConsumerFunction { it.consumeConcreteType(ConcreteType()) }
    }
    val targetType = buildee.typeVariableProducer()
    buildee.typeVariableConsumer(targetType)
    return "OK"
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
