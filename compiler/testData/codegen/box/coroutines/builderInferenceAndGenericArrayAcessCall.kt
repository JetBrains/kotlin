// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME

import kotlin.experimental.ExperimentalTypeInference

interface Foo<T>

class FooImpl<T> : Foo<T>

@OptIn(ExperimentalTypeInference::class)
fun <T> myflow(@BuilderInference block: Foo<T>.() -> Unit): Foo<T> {
    val impl = FooImpl<T>()
    impl.block()
    return impl
}


class MapWithPlusOperator<K, V>(val m: MutableMap<K, V>)

operator fun <K, V> MapWithPlusOperator<in K, in V>.plus(pair: Pair<K, V>): MapWithPlusOperator<K, V> {
    m[pair.first] = pair.second
    return this as MapWithPlusOperator<K, V>
}

var publicOk = "noOk"

fun test(map: MutableMap<Int, Any>): Foo<Any> {
    var other: MapWithPlusOperator<Int, Any> = MapWithPlusOperator(mutableMapOf())
    return myflow {
        publicOk = "OK"
        map += 1 to "s"
        other += 1 to ("s" as Any)
        map[0] = Any()
    }
}

fun box(): String {
    test(mutableMapOf(1 to ""))
    return publicOk
}