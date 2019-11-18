// IGNORE_BACKEND_FIR: JVM_IR
import kotlin.reflect.KProperty

class Delegate {
    operator fun provideDelegate(instance: Any?, property: KProperty<*>): Delegate = this
    operator fun getValue(instance: Any?, property: KProperty<*>) = "OK"
}

val result: String by Delegate()

fun box(): String {
    return result
}