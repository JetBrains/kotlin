// ISSUE: KT-54767
// WITH_STDLIB

class Buildee<T> {
    fun yield(arg: T) {}
}

fun <T> build(block: Buildee<T>.() -> Unit): Buildee<T> {
    return Buildee<T>().apply(block)
}

class TargetType()

class Klass {
    val delegatedValue by lazy {
        build {
            yield(TargetType())
        }
    }
}

fun box(): String {
    Klass().delegatedValue
    return "OK"
}
