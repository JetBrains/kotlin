// "Rename to 'setValue'" "true"
class CustomDelegate {
    operator fun get(thisRef: Any?, prop: PropertyMetadata): String = ""
    operator fun set(thisRef: Any?, prop: PropertyMetadata, value: String) {}
}

class Example {
    var a: String <caret>by CustomDelegate()
}
