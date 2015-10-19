// "Rename to 'getValue'" "true"
class CustomDelegate

operator fun CustomDelegate.get(thisRef: Any?, prop: PropertyMetadata): String = ""

class Example {
    val a: String <caret>by CustomDelegate()
}
