// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-60447

fun test() {
    val buildee = build {
        class ConcreteType
        class TargetType {
            fun consumeConcreteType(value: ConcreteType) {}
            fun targetTypeMemberFunction() {}
        }
        setTypeVariableProducerFunction { TargetType() }
        setTypeVariableConsumerFunction { it.consumeConcreteType(ConcreteType()) }
    }
    // local class equality check — turns unexpected compile-time behavior into red code
    // considered to be non-user-reproducible code for the purposes of these tests
    buildee.typeVariableProducer().targetTypeMemberFunction()
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
