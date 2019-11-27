// IGNORE_BACKEND_FIR: JVM_IR
import kotlin.reflect.KProperty

var result: String by Delegate

object Delegate {
    var value = "lol"

    operator fun getValue(instance: Any?, data: KProperty<*>): String {
        return value
    }

    operator fun setValue(instance: Any?, data: KProperty<*>, newValue: String) {
        value = newValue
    }
}

fun box(): String {
    val f = ::result
    if (f.get() != "lol") return "Fail 1: {$f.get()}"
    Delegate.value = "rofl"
    if (f.get() != "rofl") return "Fail 2: {$f.get()}"
    f.set("OK")
    return f.get()
}
