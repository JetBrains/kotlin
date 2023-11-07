// ISSUE: KT-55281

open class Buildee<T> {
    fun materialize(): T = null as T
}

class DerivedBuildee<F>: Buildee<F>()

fun <T> build(block: Buildee<T>.() -> Unit): Buildee<T> {
    return DerivedBuildee<T>().apply(block)
}

fun consume(arg: Any?) {}

fun box(): String {
    // K2: NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER
    build {
        this as DerivedBuildee<*>
        consume(materialize())
    }
    return "OK"
}
