// "Rename to 'getValue'" "true"
import kotlin.reflect.KProperty

class CustomDelegate

operator fun CustomDelegate.get(thisRef: Any?, prop: KProperty<*>): String = ""

class Example {
    val a: String <caret>by CustomDelegate()
}
