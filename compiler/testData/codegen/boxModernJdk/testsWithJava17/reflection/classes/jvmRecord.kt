// TARGET_BACKEND: JVM
// WITH_REFLECT

// Properties of a @JvmRecord class are accessed via record component accessors (`x()`), not via JavaBean getters (`getX()`).

import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaGetter
import kotlin.test.assertEquals

@JvmRecord
data class Point(val x: Int, val y: String)

fun box(): String {
    assertEquals(Point::class.java.recordComponents.map { it.accessor }, Point::class.memberProperties.sortedBy { it.name }.map { it.javaGetter })
    assertEquals(Point::class.java.getDeclaredField("x"), Point::x.javaField)

    val p = Point(1, "a")
    assertEquals(1, Point::x.call(p))
    assertEquals("a", Point::class.memberProperties.single { it.name == "y" }.getter.call(p))

    return "OK"
}
