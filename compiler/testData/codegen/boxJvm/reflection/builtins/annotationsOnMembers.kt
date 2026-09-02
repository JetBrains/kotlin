// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.KProperty
import kotlin.reflect.KMutableProperty

fun box(): String {
    // We're only checking that `annotations` doesn't crash here. After KT-13077 is fixed, it's better to render all annotations for all
    // declarations in `ReflectionIntegrationTest.testBuiltinClasses`, and remove this test.
    for (klass in listOf(Int::class, IntArray::class, Array<Any>::class, String::class, Any::class, Unit::class)) {
        klass.annotations
        for (c in klass.members) {
            c.annotations
            (c as? KProperty<*>)?.getter?.annotations
            (c as? KMutableProperty<*>)?.setter?.annotations
        }
    }
    return "OK"
}
