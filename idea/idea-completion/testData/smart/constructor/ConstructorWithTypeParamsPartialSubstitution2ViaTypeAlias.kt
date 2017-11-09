class SomeClass<K, V>
typealias TaSomeClass<V> = SomeClass<Any, V>

fun usesSomeClass(p: SomeClass<*, *>) {

}


fun usage() {
    usesSomeClass(<caret>)
}

// EXIST: {"lookupString":"TaSomeClass","tailText":"() (<root>)","typeText":"SomeClass<Any, V>"}