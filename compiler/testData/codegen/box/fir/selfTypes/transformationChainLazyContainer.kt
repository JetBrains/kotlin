// TARGET_BACKEND: JVM_IR

// WITH_STDLIB

import kotlin.Self

@Self
abstract class Lazy<T>(val computation: () -> T) {
    protected abstract fun create(computation: () -> T): Self
    fun copy(): Self = create(computation)
}

@Self
abstract class LazyContainer<T>(computation: () -> T) :
    Lazy<T, Self>(computation) {
    fun applyFunction(f: (T) -> T): Self = create { f(computation()) }
}

class LazyList<T>(computation: () -> List<T>) : LazyContainer<List<T>, LazyList<T>>(computation) {
    override fun create(computation: () -> List<T>): LazyList<T> = LazyList(computation)
    fun add(elem: T): LazyList<T> = create { computation() + elem }
}

class LazySet<T>(computation: () -> Set<T>) : LazyContainer<Set<T>, LazySet<T>>(computation) {
    override fun create(computation: () -> Set<T>): LazySet<T> = LazySet(computation)
    fun add(elem: T): LazySet<T> = create { computation() + elem }
}

fun box(): String {
    val list = LazyList { listOf(1, 2, 3) }
                .copy()
                .applyFunction { l -> l.map { it + 1 } }
                .add(15)
                .computation()

    val set = LazySet { setOf(1, 2, 3) }
                .copy()
                .applyFunction { s -> s.map { it + 1 }.toSet() }
                .add(3)
                .computation()
    val predicate = list == listOf(2, 3, 4, 15) && set == setOf(2, 3, 4)
    return if (predicate) "OK" else "ERROR"
}
