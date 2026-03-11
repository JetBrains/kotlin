// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: test/J.java
package test;

import kotlin.annotation.AnnotationTarget;

public interface J {
    @interface Primitives {
        byte b();
        char c();
        double d();
        float f();
        int i();
        long j();
        short s();
        boolean z();
    }

    @interface PrimitiveArrays {
        byte[] ba();
        char[] ca();
        double[] da();
        float[] fa();
        int[] ia();
        long[] ja();
        short[] sa();
        boolean[] za();
    }

    @interface Classes {
        Class<?> k1();
        Class<? extends Number> k2();
        Class<? super Number> k3();
        Class k4();
        Class<?>[] ka1();
        Class<? extends Number>[] ka2();
        Class<? super Number>[] ka3();
        Class[] ka4();
    }

    @interface Misc {
        String str();
        AnnotationTarget e();
        Nested a();
        String[] stra();
        AnnotationTarget[] ea();
        Nested[] aa();
    }
}

// FILE: test/Nested.java
package test;

public @interface Nested {
    String value();
}

// FILE: box.kt
import kotlin.test.assertEquals
import kotlin.reflect.KClass
import test.J

private fun KClass<*>.parametersToString(): String =
    constructors.single().parameters.joinToString("\n") { it.name + ": " + it.type }

fun box(): String {
    assertEquals(
        """
        b: kotlin.Byte
        c: kotlin.Char
        d: kotlin.Double
        f: kotlin.Float
        i: kotlin.Int
        j: kotlin.Long
        s: kotlin.Short
        z: kotlin.Boolean
        """.trimIndent(),
        J.Primitives::class.parametersToString(),
    )

    assertEquals("""
        ba: kotlin.ByteArray
        ca: kotlin.CharArray
        da: kotlin.DoubleArray
        fa: kotlin.FloatArray
        ia: kotlin.IntArray
        ja: kotlin.LongArray
        sa: kotlin.ShortArray
        za: kotlin.BooleanArray
        """.trimIndent(),
        J.PrimitiveArrays::class.parametersToString(),
    )

    if (Class.forName("kotlin.reflect.jvm.internal.SystemPropertiesKt").getMethod("getUseK1Implementation").invoke(null) == true) {
        // The flexibility difference in types doesn't matter in practice because `KClass`'s type parameter has `Any` bound.
        assertEquals(
            """
            k1: kotlin.reflect.KClass<*>
            k2: kotlin.reflect.KClass<out kotlin.Number!>
            k3: kotlin.reflect.KClass<in kotlin.Number!>
            k4: kotlin.reflect.KClass<(raw) kotlin.Any>
            ka1: kotlin.Array<kotlin.reflect.KClass<*>>
            ka2: kotlin.Array<kotlin.reflect.KClass<out kotlin.Number!>>
            ka3: kotlin.Array<kotlin.reflect.KClass<in kotlin.Number!>>
            ka4: kotlin.Array<kotlin.reflect.KClass<(raw) kotlin.Any>>
            """.trimIndent(),
            J.Classes::class.parametersToString(),
        )
    } else {
        assertEquals(
            """
            k1: kotlin.reflect.KClass<*>
            k2: kotlin.reflect.KClass<out kotlin.Number>
            k3: kotlin.reflect.KClass<in kotlin.Number>
            k4: kotlin.reflect.KClass<(raw) kotlin.Any!>
            ka1: kotlin.Array<kotlin.reflect.KClass<*>>
            ka2: kotlin.Array<kotlin.reflect.KClass<out kotlin.Number>>
            ka3: kotlin.Array<kotlin.reflect.KClass<in kotlin.Number>>
            ka4: kotlin.Array<kotlin.reflect.KClass<(raw) kotlin.Any!>>
            """.trimIndent(),
            J.Classes::class.parametersToString(),
        )
    }

    assertEquals("""
        a: test.Nested
        aa: kotlin.Array<test.Nested>
        e: kotlin.annotation.AnnotationTarget
        ea: kotlin.Array<kotlin.annotation.AnnotationTarget>
        str: kotlin.String
        stra: kotlin.Array<kotlin.String>
        """.trimIndent(),
        J.Misc::class.parametersToString(),
    )

    return "OK"
}
