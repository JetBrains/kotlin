// ISSUE: KT-57707

open class Buildee<out T>

fun <T> Buildee<T>.extensionYield(arg: T) {}

class MutableBuildee<T>: Buildee<T>() {
    fun yield(arg: T) {}
}

fun <T> build(block: MutableBuildee<T>.() -> Unit): MutableBuildee<T> {
    return MutableBuildee<T>().apply(block)
}

fun box(): String {
    build {
        yield("")
        extensionYield(1) // K1: RECEIVER_TYPE_MISMATCH (expected Int, actual String)
    }
    return "OK"
}
