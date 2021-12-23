// WITH_STDLIB
// !DIAGNOSTICS: -OPT_IN_IS_NOT_ENABLED -OPT_IN_USAGE_ERROR
// IGNORE_BACKEND: WASM

import kotlin.experimental.ExperimentalTypeInference

@OptIn(ExperimentalTypeInference::class)
fun <K, V> buildMap(@BuilderInference builderAction: MutableMap<K, V>.() -> Unit): Map<K, V> = mapOf()

fun foo(): MutableMap<CharSequence, *> = mutableMapOf<CharSequence, String>()

fun <E> MutableMap<E, *>.swap(x: MutableMap<E, *>) {}

@OptIn(ExperimentalTypeInference::class)
fun <K : V, V : CharSequence> build7(@BuilderInference builderAction: MutableMap<K, V>.() -> MutableMap<String, V>) = mutableMapOf<String, V>()

fun <K> id(x: K): K = x

@OptIn(ExperimentalStdlibApi::class)
fun main() {
    val x: Map<in String, String> = buildMap {
        put("", "")
        swap(<!ARGUMENT_TYPE_MISMATCH!>foo()<!>)
    } // `Map<CharSequence, String>` if we use builder inference, `Map<String, String>` if we don't

    val y: MutableMap<String, CharSequence> = build7 {
        <!ARGUMENT_TYPE_MISMATCH, TYPE_MISMATCH, TYPE_MISMATCH!>id(run { this })<!>
    }
}
