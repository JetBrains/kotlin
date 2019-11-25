// IGNORE_BACKEND_FIR: JVM_IR
import kotlin.reflect.KProperty

class Delegate {
    operator fun provideDelegate(thisRef: Any?, property: KProperty<*>) = this
    operator fun getValue(thisRef: Any?, property: KProperty<*>) = "OK"
}

class TestClass {
    companion object {
        val test by Delegate()
    }
}

fun box(): String {
    return TestClass.test
}