// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// IGNORE_BACKEND: JS
// IGNORE_BACKEND: NATIVE

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
