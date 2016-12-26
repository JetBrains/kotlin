// "Change 'pairs' to '*pairs'" "false"
// WITH_RUNTIME
// ACTION: Create function 'mapOf'
// ERROR: Type inference failed: fun <K, V> mapOf(pair: Pair<K, V>): Map<K, V><br>cannot be applied to<br>(Array<out Pair<String, String>>)<br>
// ERROR: Type mismatch: inferred type is Array<out Pair<String, String>> but Pair<???, ???> was expected


fun myMapOf(vararg pairs: Pair<String,String>) {
    // Does not work due to KT-15593
    val myMap = mapOf(<caret>pairs)
}