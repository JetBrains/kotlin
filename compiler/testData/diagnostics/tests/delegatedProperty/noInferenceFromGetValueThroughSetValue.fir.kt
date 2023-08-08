import kotlin.reflect.KProperty

// Definitions
class M<E>

operator fun <X> M<out X>.getValue(thisRef: Any?, property: KProperty<*>): String = "value"
operator fun <Z> M<in Z>.setValue(thisRef: Any?, property: KProperty<*>, value: Z) {}

fun <U> m(): M<U> = M()

// We don't allow to infer type of a delegate expression through a setValue, where the argument (value) is constrained by the return type of a getValue
var a by <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!><!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>m<!>()<!>

// We infer type of delegate expression through a setValue from the explicit property type
var b: String by m()

fun takeString(v: String) {}

fun main() {
    takeString(a)
    a = "a"
    takeString(b)
    b = "b"
}
