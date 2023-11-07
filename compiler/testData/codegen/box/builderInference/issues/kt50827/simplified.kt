// ISSUE: KT-50827

class Buildee<T> {
    fun yield(arg: T) {}
}

fun <T> build(block: Buildee<T>.() -> Unit): Buildee<T> {
    return Buildee<T>().apply(block)
}

class TargetType

class Box<T: Any>(val buildee: Buildee<T>)

fun box(): String {
    val box = Box(
        build {
            yield(TargetType())
        }
    )
    // K1&K2: (INITIALIZER_)TYPE_MISMATCH (expected Buildee<TargetType>, actual Buildee<Any>)
    val local: Buildee<TargetType> = box.buildee
    return "OK"
}
