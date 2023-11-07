// ISSUE: KT-56949

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

fun <T: DifferentType> consume(arg: T) {}

@Suppress("UPPER_BOUND_VIOLATION_IN_CONSTRAINT")
fun box(): String {
    build {
        yield(TargetType())
        // K1/JVM & K1/WASM & K2/JVM & K2/WASM: run-time failure (java.lang.ClassCastException)
        consume(materialize())
    }
    return "OK"
}
