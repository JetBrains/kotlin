// ISSUE: KT-63648

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

fun DifferentType.extension() {}

@Suppress("BUILDER_INFERENCE_STUB_RECEIVER")
fun box(): String {
    build {
        yield(TargetType())
        // K1: TYPE_MISMATCH (expected DifferentType, actual TargetType)
        // K2/JVM & K2/WASM: run-time failure (java.lang.ClassCastException)
        materialize().extension()
    }
    return "OK"
}
