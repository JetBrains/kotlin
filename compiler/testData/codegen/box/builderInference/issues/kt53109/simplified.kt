// ISSUE: KT-53109

class Buildee<T> {
    var assignee: (T) -> Unit = {}
}

fun <T> build(block: Buildee<T>.() -> Unit): Buildee<T> {
    return Buildee<T>().apply(block)
}

class TargetType

fun typeInfoSource(value: TargetType) {}

fun box(): String {
    build {
        // K1/JVM: could not load module <error module>
        // K1/Native & K1/WASM & K1/JS: exception during psi2ir (java.lang.NullPointerException @ org.jetbrains.kotlin.backend.common.serialization.mangle.descriptor.DescriptorExportCheckerVisitor.isExported)
        assignee = { typeInfoSource(it) }
    }
    return "OK"
}
