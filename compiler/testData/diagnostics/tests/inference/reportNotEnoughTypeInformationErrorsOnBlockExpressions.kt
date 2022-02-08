// WITH_STDLIB
// SKIP_TXT
// !DIAGNOSTICS: -CAST_NEVER_SUCCEEDS

import kotlin.experimental.ExperimentalTypeInference

class Foo7<T>

fun foo7() = null as Foo7<Int>

fun poll17(flag: Boolean): Any? {
    val inv = if (flag) { <!IMPLICIT_CAST_TO_ANY!>foo7()<!> } else { <!IMPLICIT_CAST_TO_ANY!>::<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>Foo7<!><!> }
    return inv
}

fun poll26(flag: Boolean): Any? {
    val inv = when (flag) { true -> <!IMPLICIT_CAST_TO_ANY, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>::Foo7<!> false -> <!IMPLICIT_CAST_TO_ANY!>foo7()<!> <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> <!IMPLICIT_CAST_TO_ANY, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>::Foo7<!> }
    return inv
}

fun poll36(flag: Boolean): Any? {
    val inv = when (flag) { true -> <!IMPLICIT_CAST_TO_ANY, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>::Foo7<!> false -> <!IMPLICIT_CAST_TO_ANY!>foo7()<!> }
    return inv
}

fun poll56(): Any? {
    val inv = try { ::<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>Foo7<!> } catch (e: Exception) { foo7() } finally { foo7() }
    return inv
}
