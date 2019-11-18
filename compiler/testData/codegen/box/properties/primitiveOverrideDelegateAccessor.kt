// IGNORE_BACKEND_FIR: JVM_IR
import kotlin.reflect.KProperty

class Holder(var value: Int) {
    operator fun getValue(that: Any?, desc: KProperty<*>) = value
    operator fun setValue(that: Any?, desc: KProperty<*>, newValue: Int) { value = newValue }
}

interface R<T: Comparable<T>> {
    var value: T
}

class A(start: Int) : R<Int> {
    override var value: Int by Holder(start)
}

fun box(): String {
    val a = A(239)
    a.value = 42
    return if (a.value == 42) "OK" else "Fail 1"
}
