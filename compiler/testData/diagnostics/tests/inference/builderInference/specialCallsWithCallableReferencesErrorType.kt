// WITH_RUNTIME
// SKIP_TXT
// !DIAGNOSTICS: -CAST_NEVER_SUCCEEDS -UNCHECKED_CAST -UNUSED_PARAMETER -UNUSED_VARIABLE -EXPERIMENTAL_API_USAGE_ERROR -UNUSED_EXPRESSION

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

fun <L> flow(@BuilderInference block: suspend FlowCollector<L>.() -> Unit) = Flow(block)

class Flow<out R>(private val block: suspend FlowCollector<R>.() -> Unit)


fun poll7(): Flow<String> {
    return flow {
        val inv = <!TYPE_INFERENCE_POSTPONED_VARIABLE_IN_RECEIVER_TYPE!>::bar<!><!NOT_NULL_ASSERTION_ON_CALLABLE_REFERENCE!>!!<!>
        <!DEBUG_INFO_MISSING_UNRESOLVED!>inv()<!>
    }
}

fun poll71(): Flow<String> {
    return flow {
        val inv = ::bar2<!NOT_NULL_ASSERTION_ON_CALLABLE_REFERENCE!>!!<!>
        inv()
    }
}

fun poll72(): Flow<String> {
    return flow {
        val inv = <!TYPE_INFERENCE_POSTPONED_VARIABLE_IN_RECEIVER_TYPE!>::bar3<!><!NOT_NULL_ASSERTION_ON_CALLABLE_REFERENCE!>!!<!>
        <!DEBUG_INFO_MISSING_UNRESOLVED!>inv()<!>
    }
}

fun poll73(): Flow<String> {
    return flow {
        val inv = ::bar4<!NOT_NULL_ASSERTION_ON_CALLABLE_REFERENCE!>!!<!>
        inv
    }
}

fun poll74(): Flow<String> {
    return flow {
        val inv = <!TYPE_INFERENCE_POSTPONED_VARIABLE_IN_RECEIVER_TYPE!>::bar5<!><!NOT_NULL_ASSERTION_ON_CALLABLE_REFERENCE!>!!<!>
        <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>inv<!>
    }
}

fun poll75(): Flow<String> {
    return flow {
        val inv = ::Foo6<!NOT_NULL_ASSERTION_ON_CALLABLE_REFERENCE!>!!<!>
        inv
    }
}

fun poll76(): Flow<String> {
    return flow {
        val inv = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>::<!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>Foo7<!><!><!NOT_NULL_ASSERTION_ON_CALLABLE_REFERENCE!>!!<!>
        <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>inv<!>
    }
}

