// IGNORE_BACKEND_FIR: JVM_IR
import kotlin.reflect.KProperty

class Delegate(val value: String) {
    operator fun provideDelegate(instance: A, property: KProperty<*>): Delegate = Delegate(instance.value)
    operator fun getValue(instance: Any?, property: KProperty<*>) = value
}

class A(val value: String) {
    val result: String by Delegate("Fail")
}

fun box(): String {
    return A("OK").result
}