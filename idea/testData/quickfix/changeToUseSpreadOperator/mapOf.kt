// "Change 'pairs' to '*pairs'" "true"
// WITH_RUNTIME

fun <K, V> yourMapOf(vararg pairs: Pair<K, V>) {}

fun myMapOf(vararg pairs: Pair<String,String>) {
    val myMap = yourMapOf(<caret>pairs)
}