// IS_APPLICABLE: false
import kotlin.reflect.KProperty

class C {
    <caret>var foo by Delegate
}

object Delegate {
    operator fun getValue(instance: Any?, property: KProperty<*>): String = ""
    operator fun setValue(instance: Any?, property: KProperty<*>, value: String) {}
}