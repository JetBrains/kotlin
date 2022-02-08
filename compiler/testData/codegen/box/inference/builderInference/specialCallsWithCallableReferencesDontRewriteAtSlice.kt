// WITH_STDLIB
// SKIP_TXT
// !DIAGNOSTICS: -CAST_NEVER_SUCCEEDS -UNCHECKED_CAST -UNUSED_PARAMETER -UNUSED_VARIABLE -OPT_IN_USAGE_ERROR -UNUSED_EXPRESSION

import kotlin.experimental.ExperimentalTypeInference

fun <K> FlowCollector<K>.bar(): K = null as K
fun <K> FlowCollector<K>.foo(): K = null as K

fun <K> K.bar3(): K = null as K
fun <K> K.foo3(): K = null as K

fun bar2(): Int = 1
fun foo2(): Float = 1f

val bar4: Int
    get() = 1

var foo4: Float
    get() = 1f
    set(value) {}

val <K> FlowCollector<K>.bar5: K get() = null as K
val <K> FlowCollector<K>.foo5: K get() = null as K

class Foo6

class Foo7<T>
fun foo7() = null as Foo7<Int>

interface FlowCollector<in T> {}

@OptIn(ExperimentalTypeInference::class)
fun <L> flow(@BuilderInference block: suspend FlowCollector<L>.() -> Unit) = Flow(block)

class Flow<out R>(private val block: suspend FlowCollector<R>.() -> Unit)

fun poll71(): Flow<String> {
    return flow {
        val inv = ::bar2!!
        inv()
    }
}

fun poll73(): Flow<String> {
    return flow {
        val inv = ::bar4!!
        inv
    }
}

fun poll75(): Flow<String> {
    return flow {
        val inv = ::Foo6!!
        inv
    }
}

fun poll85(): Flow<String> {
    return flow {
        val inv = ::Foo6 in setOf(::Foo6)
        inv
    }
}

fun box() = "OK"
