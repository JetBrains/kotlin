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

fun <L> flow(@BuilderInference block: suspend FlowCollector<L>.() -> Unit) = Flow(block)

class Flow<out R>(private val block: suspend FlowCollector<R>.() -> Unit)

fun poll1(flag: Boolean): Flow<String> {
    return flow {
        val inv = if (flag) { <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>::<!DEBUG_INFO_MISSING_UNRESOLVED!>bar2<!><!> } else { <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>::<!DEBUG_INFO_MISSING_UNRESOLVED!>foo2<!><!> }
        <!DEBUG_INFO_MISSING_UNRESOLVED!>inv<!>()
    }
}

fun poll11(flag: Boolean): Flow<String> {
    return flow {
        val inv = if (flag) { <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>::<!DEBUG_INFO_MISSING_UNRESOLVED!>bar2<!><!> } else { <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>::<!DEBUG_INFO_MISSING_UNRESOLVED!>foo2<!><!> }
        <!DEBUG_INFO_MISSING_UNRESOLVED!>inv<!>()
    }
}

fun poll12(flag: Boolean): Flow<String> {
    return flow {
        val inv = if (flag) { <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>::<!DEBUG_INFO_MISSING_UNRESOLVED!>bar3<!><!> } else { <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>::<!DEBUG_INFO_MISSING_UNRESOLVED!>foo3<!><!> }
        <!DEBUG_INFO_MISSING_UNRESOLVED!>inv<!>()
    }
}

fun poll13(flag: Boolean): Flow<String> {
    return flow {
        val inv = if (flag) { <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>::<!DEBUG_INFO_MISSING_UNRESOLVED!>bar2<!><!> } else { <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>::<!DEBUG_INFO_MISSING_UNRESOLVED!>foo3<!><!> }
        <!DEBUG_INFO_MISSING_UNRESOLVED!>inv<!>()
    }
}

fun poll14(flag: Boolean): Flow<String> {
    return flow {
        val inv = if (flag) { <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>::<!DEBUG_INFO_MISSING_UNRESOLVED!>bar4<!><!> } else { <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>::<!DEBUG_INFO_MISSING_UNRESOLVED!>foo4<!><!> }
        <!DEBUG_INFO_MISSING_UNRESOLVED!>inv<!>()
    }
}

fun poll15(flag: Boolean): Flow<String> {
    return flow {
        val inv = if (flag) { <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>::<!DEBUG_INFO_MISSING_UNRESOLVED!>bar5<!><!> } else { <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>::<!DEBUG_INFO_MISSING_UNRESOLVED!>foo5<!><!> }
        <!DEBUG_INFO_MISSING_UNRESOLVED!>inv<!>()
    }
}

fun poll16(flag: Boolean): Flow<String> {
    return flow {
        val inv = if (flag) { <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>::<!DEBUG_INFO_MISSING_UNRESOLVED!>Foo6<!><!> } else { <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>::<!DEBUG_INFO_MISSING_UNRESOLVED!>Foo6<!><!> }
        <!DEBUG_INFO_MISSING_UNRESOLVED!>inv<!>()
    }
}

fun poll17(flag: Boolean): Flow<String> {
    return flow {
        val inv = if (flag) { foo7() } else { ::<!DEBUG_INFO_MISSING_UNRESOLVED!>Foo7<!> }
        inv
    }
}

fun poll4(): Flow<String> {
    return flow {
        val inv = try { <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>::<!DEBUG_INFO_MISSING_UNRESOLVED!>bar<!><!> } finally { ::<!DEBUG_INFO_MISSING_UNRESOLVED!>foo<!> }
        <!DEBUG_INFO_MISSING_UNRESOLVED!>inv<!>()
    }
}

fun poll41(): Flow<String> {
    return flow {
        val inv = try { <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>::<!DEBUG_INFO_MISSING_UNRESOLVED!>bar2<!><!> } finally { ::<!DEBUG_INFO_MISSING_UNRESOLVED!>foo2<!> }
        <!DEBUG_INFO_MISSING_UNRESOLVED!>inv<!>()
    }
}

fun poll42(): Flow<String> {
    return flow {
        val inv = try { <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>::<!DEBUG_INFO_MISSING_UNRESOLVED!>bar3<!><!> } finally { ::<!DEBUG_INFO_MISSING_UNRESOLVED!>foo3<!> }
        <!DEBUG_INFO_MISSING_UNRESOLVED!>inv<!>()
    }
}

fun poll43(): Flow<String> {
    return flow {
        val inv = try { <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>::<!DEBUG_INFO_MISSING_UNRESOLVED!>bar4<!><!> } finally { ::<!DEBUG_INFO_MISSING_UNRESOLVED!>foo4<!> }
        <!DEBUG_INFO_MISSING_UNRESOLVED!>inv<!>()
    }
}

fun poll44(): Flow<String> {
    return flow {
        val inv = try { <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>::<!DEBUG_INFO_MISSING_UNRESOLVED!>bar5<!><!> } finally { ::<!DEBUG_INFO_MISSING_UNRESOLVED!>foo5<!> }
        <!DEBUG_INFO_MISSING_UNRESOLVED!>inv<!>()
    }
}

fun poll45(): Flow<String> {
    return flow {
        val inv = try { <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>::<!DEBUG_INFO_MISSING_UNRESOLVED!>Foo6<!><!> } finally { ::<!DEBUG_INFO_MISSING_UNRESOLVED!>Foo6<!> }
        <!DEBUG_INFO_MISSING_UNRESOLVED!>inv<!>()
    }
}

fun poll46(): Flow<String> {
    return flow {
        val inv = try { foo7() } finally { ::<!DEBUG_INFO_MISSING_UNRESOLVED!>Foo7<!> }
        inv
    }
}

fun poll5(): Flow<String> {
    return flow {
        val inv = try { <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>::<!DEBUG_INFO_MISSING_UNRESOLVED!>bar<!><!> } catch (e: Exception) { <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>::<!DEBUG_INFO_MISSING_UNRESOLVED!>foo<!><!> } finally { ::<!DEBUG_INFO_MISSING_UNRESOLVED!>foo<!> }
        <!DEBUG_INFO_MISSING_UNRESOLVED!>inv<!>()
    }
}

fun poll51(): Flow<String> {
    return flow {
        val inv = try { <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>::<!DEBUG_INFO_MISSING_UNRESOLVED!>bar2<!><!> } catch (e: Exception) { <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>::<!DEBUG_INFO_MISSING_UNRESOLVED!>foo2<!><!> } finally { ::<!DEBUG_INFO_MISSING_UNRESOLVED!>foo2<!> }
        <!DEBUG_INFO_MISSING_UNRESOLVED!>inv<!>()
    }
}

fun poll52(): Flow<String> {
    return flow {
        val inv = try { <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>::<!DEBUG_INFO_MISSING_UNRESOLVED!>bar3<!><!> } catch (e: Exception) { <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>::<!DEBUG_INFO_MISSING_UNRESOLVED!>foo3<!><!> } finally { ::<!DEBUG_INFO_MISSING_UNRESOLVED!>foo3<!> }
        <!DEBUG_INFO_MISSING_UNRESOLVED!>inv<!>()
    }
}

fun poll53(): Flow<String> {
    return flow {
        val inv = try { <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>::<!DEBUG_INFO_MISSING_UNRESOLVED!>bar4<!><!> } catch (e: Exception) { <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>::<!DEBUG_INFO_MISSING_UNRESOLVED!>foo4<!><!> } finally { ::<!DEBUG_INFO_MISSING_UNRESOLVED!>foo4<!> }
        <!DEBUG_INFO_MISSING_UNRESOLVED!>inv<!>()
    }
}

fun poll54(): Flow<String> {
    return flow {
        val inv = try { <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>::<!DEBUG_INFO_MISSING_UNRESOLVED!>bar5<!><!> } catch (e: Exception) { <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>::<!DEBUG_INFO_MISSING_UNRESOLVED!>foo5<!><!> } finally { ::<!DEBUG_INFO_MISSING_UNRESOLVED!>foo5<!> }
        <!DEBUG_INFO_MISSING_UNRESOLVED!>inv<!>()
    }
}

fun poll55(): Flow<String> {
    return flow {
        val inv = try { <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>::<!DEBUG_INFO_MISSING_UNRESOLVED!>Foo6<!><!> } catch (e: Exception) { <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>::<!DEBUG_INFO_MISSING_UNRESOLVED!>Foo6<!><!> } finally { ::<!DEBUG_INFO_MISSING_UNRESOLVED!>Foo6<!> }
        <!DEBUG_INFO_MISSING_UNRESOLVED!>inv<!>()
    }
}

fun poll56(): Flow<String> {
    return flow {
        val inv = try { ::<!DEBUG_INFO_MISSING_UNRESOLVED!>Foo7<!> } catch (e: Exception) { foo7() } finally { foo7() }
        inv
    }
}
