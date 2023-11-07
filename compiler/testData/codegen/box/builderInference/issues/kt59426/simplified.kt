// ISSUE: KT-59426

class Buildee<T: Any> {
    private lateinit var data: T
    fun yield(arg: T) { data = arg }
    fun materialize(): T = data
}

fun <T: Any> build(block: Buildee<T>.() -> Unit): Buildee<T> {
    return Buildee<T>().apply(block)
}

class TargetType
class DifferentType

fun Buildee<TargetType>.extension() {
    // K2/JVM & K2/WASM: run-time failure (java.lang.ClassCastException)
    val local: TargetType = materialize()
}

fun box(): String {
    build {
        yield(DifferentType()) // K1: TYPE_MISMATCH (expected TargetType, actual DifferentType)
        extension() // K1: RECEIVER_TYPE_MISMATCH (expected TargetType, actual DifferentType)
    }
    return "OK"
}
