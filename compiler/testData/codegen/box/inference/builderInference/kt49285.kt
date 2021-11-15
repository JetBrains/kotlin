// DONT_TARGET_EXACT_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
// WITH_STDLIB

import kotlin.experimental.ExperimentalTypeInference

class Foo<T> {
    fun add(x: T) {}
}

@OptIn(ExperimentalTypeInference::class)
fun <K1> myBuilder1(@BuilderInference builder: Foo<K1>.() -> Foo<K1>): Foo<K1> = Foo<K1>().apply { builder() }

@OptIn(ExperimentalTypeInference::class)
fun <K2> myBuilder2(@BuilderInference builder: Foo<K2>.() -> Unit): Foo<K2> = Foo<K2>().apply(builder)

fun box(): String {
    val result1 = myBuilder1 {
        add(null)
        myBuilder2 {
            add("")
        }
    }
    return "OK"
}