// WITH_STDLIB
// SKIP_TXT
// !DIAGNOSTICS: -CAST_NEVER_SUCCEEDS

import kotlin.experimental.ExperimentalTypeInference

class Foo7<T>

fun foo7() = null as Foo7<Int>

fun poll17(flag: Boolean): Any? {
    val inv = if (flag) { foo7() } else { ::Foo7 }
    return inv
}

fun poll26(flag: Boolean): Any? {
    val inv = when (flag) { true -> ::Foo7 false -> foo7() else -> ::Foo7 }
    return inv
}

fun poll36(flag: Boolean): Any? {
    val inv = when (flag) { true -> ::Foo7 false -> foo7() }
    return inv
}

fun poll56(): Any? {
    val inv = try { ::Foo7 } catch (e: Exception) { foo7() } finally { foo7() }
    return inv
}
