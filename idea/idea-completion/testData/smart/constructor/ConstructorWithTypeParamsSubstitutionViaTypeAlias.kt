class SomeClass<K, V>
typealias TaSomeClass = SomeClass<Any, Any>

fun usesSomeClass(p: SomeClass<*, *>) {

}


fun usage() {
    usesSomeClass(<caret>)
}

// EXIST: {"lookupString":"TaSomeClass","tailText":"() (<root>)","typeText":"SomeClass<Any, Any>"}