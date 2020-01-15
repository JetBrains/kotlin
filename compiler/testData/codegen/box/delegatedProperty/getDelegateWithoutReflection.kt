// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS
// IGNORE_BACKEND: NATIVE
// IGNORE_BACKEND_FIR: JVM_IR

// No kotlin-reflect.jar in this test
// WITH_RUNTIME

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
