// ISSUE: KT-53109

// IGNORE_LIGHT_ANALYSIS
// IGNORE_BACKEND_K1: ANY
// REASON: compile-time failure in K1/JVM (java.lang.AssertionError: Could not load module <Error module> @ org.jetbrains.kotlin.backend.common.serialization.KotlinIrLinker.resolveModuleDeserializer)
// REASON: compile-time failure in K1/Native, K1/WASM, K1/JS (java.lang.NullPointerException @ org.jetbrains.kotlin.backend.common.serialization.mangle.descriptor.DescriptorExportCheckerVisitor.isExported)

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
