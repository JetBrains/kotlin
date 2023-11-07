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
    // K1: could not load module <error module>
    // K2/Native: org.jetbrains.kotlin.backend.common.linkage.issues.IrDisallowedErrorNode: Class found but error nodes are not allowed.
    build {
        assignee = { typeInfoSource(it) }
    }
    return "OK"
}
