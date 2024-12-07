// TARGET_BACKEND: JVM
// TARGET_BACKEND: JVM_IR

// No kotlin-reflect.jar in this test
// WITH_STDLIB

import kotlin.reflect.KProperty

object Delegate {
    operator fun getValue(instance: Any?, property: KProperty<*>) = ""
}

val foo: String by Delegate

fun box(): String {
    try {
        ::foo.getDelegate()
        return "Fail: error should have been thrown"
    }
    catch (e: KotlinReflectionNotSupportedError) {
        return "OK"
    }
}
