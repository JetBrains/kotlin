// ISSUE: KT-55252

// IGNORE_BACKEND_K1: NATIVE
// REASON: compile-time failure (java.lang.NullPointerException @ org.jetbrains.kotlin.library.metadata.KlibModuleOriginKt.getKlibModuleOrigin)

fun box(): String {
    build {
        setTypeVariableProducerFunction {}
    }
    return "OK"
}




class Buildee<TV> {
    var typeVariableProducer: () -> TV = { storage }
    fun setTypeVariableProducerFunction(producer: () -> TV) { typeVariableProducer = producer }
    private var storage: TV = Unit as TV
}

fun <PTV> build(instructions: Buildee<PTV>.() -> Unit): Buildee<PTV> {
    return Buildee<PTV>().apply(instructions)
}
