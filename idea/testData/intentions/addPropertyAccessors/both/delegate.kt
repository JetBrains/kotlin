// IS_APPLICABLE: false
import kotlin.reflect.KProperty
class Test {
    var x<caret>: String by Delegate()
}
class Delegate {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): String = ""
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {}
}
