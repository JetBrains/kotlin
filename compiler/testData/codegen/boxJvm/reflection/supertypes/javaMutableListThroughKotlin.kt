// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: J.java
import java.io.Serializable;
import java.util.List;

public interface J {
    interface J1 extends List<List<String>> {}
    interface J2 extends List<List<? extends Number>> {}
    interface J3 extends List<List<? super Number>> {}
    interface J4<JT> extends List<List<JT>> {}
    interface J5<JS> extends List<List<JS[]>> {}
    interface J6<JW extends Number & Serializable> extends List<List<?>> {}
}

// FILE: box.kt
import java.io.Serializable
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.allSupertypes
import kotlin.test.assertEquals

interface K1 : J.J1
interface K2 : J.J2
interface K3 : J.J3
interface K4<T> : J.J4<T>
interface K5<S> : J.J5<S>
interface K6<W> : J.J6<W> where W : Number, W : Serializable

private fun KClass<*>.supertype(): KType =
    allSupertypes.single { it.classifier == List::class }

fun box(): String {
    assertEquals("kotlin.collections.MutableList<kotlin.collections.(Mutable)List<kotlin.String!>!>", K1::class.supertype().toString())

    if (Class.forName("kotlin.reflect.jvm.internal.SystemPropertiesKt").getMethod("getUseK1Implementation").invoke(null) == true) {
        // It's effectively the same type as below, because `List<out S>` is the same type as `List<S>` (because of declaration-site
        // variance in `kotlin.collections.List`.
        // But K1 implementation in `JavaTypeResolver` used the following logic: do not add a projection at use site, if the same projection
        // ("out" here) is present at declaration site.
        assertEquals(
            "kotlin.collections.MutableList<(kotlin.collections.MutableList<out kotlin.Number!>..kotlin.collections.List<kotlin.Number!>?)>",
            K2::class.supertype().toString(),
        )
    } else {
        assertEquals(
            "kotlin.collections.MutableList<kotlin.collections.(Mutable)List<out kotlin.Number!>!>",
            K2::class.supertype().toString(),
        )
    }

    assertEquals("kotlin.collections.MutableList<kotlin.collections.MutableList<in kotlin.Number!>!>", K3::class.supertype().toString())
    assertEquals("kotlin.collections.MutableList<kotlin.collections.(Mutable)List<T!>!>", K4::class.supertype().toString())
    assertEquals("kotlin.collections.MutableList<kotlin.collections.(Mutable)List<kotlin.Array<(out) S!>!>!>", K5::class.supertype().toString())

    assertEquals("kotlin.collections.MutableList<kotlin.collections.(Mutable)List<*>!>", K6::class.supertype().toString())

    return "OK"
}
