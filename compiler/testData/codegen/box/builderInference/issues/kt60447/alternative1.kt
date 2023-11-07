// ISSUE: KT-60447

class Buildee<T> {
    fun yield(arg: T) {}
    fun execute(block: (T) -> Unit) {}
}

fun <T> build(block: Buildee<T>.() -> Unit): Buildee<T> {
    return Buildee<T>().apply(block)
}

class Placeholder
class TargetType { var variable = Placeholder() }

fun box(): String {
    build {
        yield(TargetType())
        execute { it.variable = Placeholder() } // K1&K2: UNRESOLVED_REFERENCE
    }
    return "OK"
}
