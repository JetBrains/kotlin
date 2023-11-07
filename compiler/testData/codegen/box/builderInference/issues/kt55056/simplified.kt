// ISSUE: KT-55056

open class Buildee<out T> {
    fun materialize(): T = "" as T
}

fun <T> Buildee<T>.extensionYield(arg: T): Buildee<T> { return this }

class MutableBuildee<T>: Buildee<T>() {
    fun yield(arg: T) {}
}

fun <T> build(block: MutableBuildee<T>.() -> Unit): MutableBuildee<T> {
    return MutableBuildee<T>().apply(block)
}

fun box(): String {
    build {
        yield("")
        // K1: RECEIVER_TYPE_MISMATCH (expected Int, actual String)
        // K2/JVM & K2/WASM: run-time failure (java.lang.ClassCastException)
        val local: Int = extensionYield(1).materialize()
    }
    return "OK"
}
