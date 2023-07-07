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

fun <L> flow(block: suspend FlowCollector<L>.() -> Unit) = Flow(block)

class Flow<out R>(private val block: suspend FlowCollector<R>.() -> Unit)

fun poll1(flag: Boolean): Flow<String> {
    return flow {
        val inv = if (flag) { ::bar2 } else { ::foo2 }
        inv()
    }
}

fun poll11(flag: Boolean): Flow<String> {
    return flow {
        val inv = if (flag) { ::bar2 } else { ::foo2 }
        inv()
    }
}

fun poll12(flag: Boolean): Flow<String> {
    return flow {
        val inv = if (flag) { ::bar3 } else { ::foo3 }
        inv()
    }
}

fun poll13(flag: Boolean): Flow<String> {
    return flow {
        val inv = if (flag) { ::bar2 } else { ::foo3 }
        inv()
    }
}

fun poll14(flag: Boolean): Flow<String> {
    return flow {
        val inv = if (flag) { ::bar4 } else { ::foo4 }
        inv()
    }
}

fun poll15(flag: Boolean): Flow<String> {
    return flow {
        val inv = if (flag) { ::bar5 } else { ::foo5 }
        inv()
    }
}

fun poll16(flag: Boolean): Flow<String> {
    return flow {
        val inv = if (flag) { ::Foo6 } else { ::Foo6 }
        inv()
    }
}

fun poll17(flag: Boolean): Flow<String> {
    return flow {
        val inv = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>if (flag) { foo7() } else { ::Foo7 }<!>
        inv
    }
}

fun poll4(): Flow<String> {
    return flow {
        val inv = try { ::bar } finally { ::foo }
        inv()
    }
}

fun poll41(): Flow<String> {
    return flow {
        val inv = try { ::bar2 } finally { ::foo2 }
        inv()
    }
}

fun poll42(): Flow<String> {
    return flow {
        val inv = try { ::bar3 } finally { ::foo3 }
        inv()
    }
}

fun poll43(): Flow<String> {
    return flow {
        val inv = try { ::bar4 } finally { ::foo4 }
        inv()
    }
}

fun poll44(): Flow<String> {
    return flow {
        val inv = try { ::bar5 } finally { ::foo5 }
        inv()
    }
}

fun poll45(): Flow<String> {
    return flow {
        val inv = try { ::Foo6 } finally { ::Foo6 }
        inv()
    }
}

fun poll46(): Flow<String> {
    return flow {
        val inv = try { foo7() } finally { ::<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>Foo7<!> }
        inv
    }
}

fun poll5(): Flow<String> {
    return flow {
        val inv = try { ::bar } catch (e: Exception) { ::foo } finally { ::foo }
        inv()
    }
}

fun poll51(): Flow<String> {
    return flow {
        val inv = try { ::bar2 } catch (e: Exception) { ::foo2 } finally { ::foo2 }
        inv()
    }
}

fun poll52(): Flow<String> {
    return flow {
        val inv = try { ::bar3 } catch (e: Exception) { ::foo3 } finally { ::foo3 }
        inv()
    }
}

fun poll53(): Flow<String> {
    return flow {
        val inv = try { ::bar4 } catch (e: Exception) { ::foo4 } finally { ::foo4 }
        inv()
    }
}

fun poll54(): Flow<String> {
    return flow {
        val inv = try { ::bar5 } catch (e: Exception) { ::foo5 } finally { ::foo5 }
        inv()
    }
}

fun poll55(): Flow<String> {
    return flow {
        val inv = try { ::Foo6 } catch (e: Exception) { ::Foo6 } finally { ::Foo6 }
        inv()
    }
}

fun poll56(): Flow<String> {
    return flow {
        val inv = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>try { ::Foo7 } catch (e: Exception) { foo7() } finally { foo7() }<!>
        inv
    }
}
