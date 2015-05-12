package m

interface Element

fun test(handlers: Map<String, Element.()->Unit>) {

    handlers.getOrElse("name", { null })
}

fun <K,V> Map<K,V>.getOrElse(key: K, defaultValue: ()-> V) : V = throw Exception("$key $defaultValue")