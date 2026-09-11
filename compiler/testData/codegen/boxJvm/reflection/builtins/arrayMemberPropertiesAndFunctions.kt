// TARGET_BACKEND: JVM
// WITH_REFLECT
// Tests that array types expose their 'size' memberProperty and inherited
// Object methods via memberFunctions.

import kotlin.reflect.full.*
import kotlin.test.*

fun box(): String {
    val arrayClasses = listOf(
        Array::class, IntArray::class, LongArray::class, ShortArray::class,
        ByteArray::class, FloatArray::class, DoubleArray::class,
        BooleanArray::class, CharArray::class
    )

    for (klass in arrayClasses) {
        // memberProperties must include 'size'
        assertTrue(klass.memberProperties.any { it.name == "size" },
            "${klass.simpleName}::class.memberProperties must include 'size'")

        // memberFunctions must include the inherited Object methods
        val fnNames = klass.memberFunctions.map { it.name }.toSet()
        for (name in listOf("equals", "hashCode", "toString")) {
            assertTrue(name in fnNames,
                "${klass.simpleName}::class.memberFunctions must include '$name'")
        }
    }

    return "OK"
}
