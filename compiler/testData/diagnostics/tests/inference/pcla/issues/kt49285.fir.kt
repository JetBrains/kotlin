// WITH_STDLIB

import kotlin.experimental.ExperimentalTypeInference

class Foo<T> {
    fun add(x: T) {}
}

@OptIn(ExperimentalTypeInference::class)
fun <K1> myBuilder1(builder: Foo<K1>.() -> Foo<K1>): Foo<K1> = Foo<K1>().apply { builder() }

@OptIn(ExperimentalTypeInference::class)
fun <K2> myBuilder2(builder: Foo<K2>.() -> Unit): Foo<K2> = Foo<K2>().apply(builder)

val result1 = myBuilder1 {
    add(null)
    myBuilder2 {
        add("")
    }
}
