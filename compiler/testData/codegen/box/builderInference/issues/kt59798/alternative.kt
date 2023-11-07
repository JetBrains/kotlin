// ISSUE: KT-59798

class Buildee<T> {
    fun yield(arg: T) {}
    fun materialize(): T = TargetType() as T
}

fun <T> build(block: Buildee<T>.() -> Unit): Buildee<T> {
    return Buildee<T>().apply(block)
}

class TargetType

fun box(): String {
    build {
        yield(TargetType())
        materialize()
    }
    build {
        yield(TargetType())
        materialize().let {} // K1: BUILDER_INFERENCE_STUB_RECEIVER
    }
    return "OK"
}
