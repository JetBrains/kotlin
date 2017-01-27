class SomeClass<K, V>
typealias TaSomeClass<K, V> = SomeClass<K, V>

fun <T> usesSomeClass(p: SomeClass<T, *>) {

}


fun usage() {
    usesSomeClass<Any>(<caret>)
}

// EXIST: {"lookupString":"TaSomeClass","tailText":"() (<root>)","typeText":"SomeClass<K, V>"}