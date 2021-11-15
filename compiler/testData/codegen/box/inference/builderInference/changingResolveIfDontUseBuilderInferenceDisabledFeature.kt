// WITH_STDLIB
// !LANGUAGE: -UseBuilderInferenceOnlyIfNeeded

import kotlin.experimental.ExperimentalTypeInference

@OptIn(ExperimentalTypeInference::class)
fun <K, V> buildMap(@BuilderInference builderAction: MutableMap<K, V>.() -> Unit): Map<K, V> = mapOf()

fun foo(): MutableMap<CharSequence, *> = mutableMapOf<CharSequence, String>()

fun <E> MutableMap<E, *>.swap(x: MutableMap<E, *>) {}

@OptIn(ExperimentalStdlibApi::class)
fun box(): String {
    val x: Map<in String, String> = buildMap {
        put("", "")
        swap(foo())
    } // `Map<CharSequence, String>` if we use builder inference, `Map<String, String>` if we don't
    return "OK"
}
