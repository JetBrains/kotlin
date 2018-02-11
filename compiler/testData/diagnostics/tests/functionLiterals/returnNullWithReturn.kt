// !WITH_NEW_INFERENCE
package m

interface Element

fun test(handlers: Map<String, Element.()->Unit>) {

    handlers.getOrElse("name", l@ { return@l <!OI;NULL_FOR_NONNULL_TYPE!>null<!> })
}

fun <K,V> Map<K,V>.getOrElse(key: K, defaultValue: ()-> V) : V = throw Exception("$key $defaultValue")