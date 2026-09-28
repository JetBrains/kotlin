// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.KCallable
import kotlin.reflect.KProperty
import kotlin.reflect.KMutableProperty

fun box(): String {
    fun check(c: KCallable<*>?) {
        c?.annotations
        c?.parameters?.forEach { it.annotations }
    }

    // We're only checking that `annotations` doesn't crash here. After KT-13077 is fixed, it's better to render all annotations for all
    // declarations in `ReflectionIntegrationTest.testBuiltinClasses`, and remove this test.
    for (klass in listOf(Int::class, IntArray::class, Array<Any>::class, String::class, Any::class, Unit::class)) {
        klass.annotations
        for (c in klass.members) {
            check(c)
            check((c as? KProperty<*>)?.getter)
            check((c as? KMutableProperty<*>)?.setter)
        }
    }
    return "OK"
}
