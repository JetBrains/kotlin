// ISSUE: KT-60447

class Buildee<T> {
    fun yieldProducer(lambda: () -> T) {}
    fun yieldConsumer(lambda: T.() -> Unit) {}
}

fun <T> build(block: Buildee<T>.() -> Unit): Buildee<T> {
    return Buildee<T>().apply(block)
}

fun box(): String {
    build {
        class Placeholder
        class TargetType { var variable = Placeholder() }
        yieldProducer { TargetType() }
        yieldConsumer { variable = Placeholder() } // K1&K2: UNRESOLVED_REFERENCE
    }
    return "OK"
}
