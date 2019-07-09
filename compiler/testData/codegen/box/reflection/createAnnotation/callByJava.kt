// TARGET_BACKEND: JVM

// WITH_REFLECT
// FILE: J.java

public interface J {
    @interface NoParams {}

    @interface OneDefault {
        String s() default "OK";
    }

    @interface OneNonDefault {
        String s();
    }

    @interface TwoParamsOneDefault {
        String s();
        int x() default 42;
    }

    @interface TwoNonDefaults {
        String string();
        Class<?> clazz();
    }

    @interface ManyDefaultParams {
        int i() default 0;
        String s() default "";
        double d() default 3.14;
    }
}

// FILE: K.kt

import J.*
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor
import kotlin.test.assertEquals
import kotlin.test.assertFails

inline fun <reified T : Annotation> create(args: Map<String, Any?>): T {
    val ctor = T::class.constructors.single()
    return ctor.callBy(args.mapKeys { entry -> ctor.parameters.single { it.name == entry.key } })
}

inline fun <reified T : Annotation> create(): T = create(emptyMap())

fun box(): String {
    create<NoParams>()

    val t1 = create<OneDefault>()
    assertEquals("OK", t1.s)
    assertFails { create<OneDefault>(mapOf("s" to 42)) }

    val t2 = create<OneNonDefault>(mapOf("s" to "OK"))
    assertEquals("OK", t2.s)
    assertFails { create<OneNonDefault>() }

    val t3 = create<TwoParamsOneDefault>(mapOf("s" to "OK"))
    assertEquals("OK", t3.s)
    assertEquals(42, t3.x)
    val t4 = create<TwoParamsOneDefault>(mapOf("s" to "OK", "x" to 239))
    assertEquals(239, t4.x)
    assertFails { create<TwoParamsOneDefault>(mapOf("s" to "Fail", "x" to "Fail")) }

    assertFails("KClass (not Class) instances should be passed as arguments") {
        create<TwoNonDefaults>(mapOf("clazz" to String::class.java, "string" to "Fail"))
    }

    val t5 = create<TwoNonDefaults>(mapOf("clazz" to String::class, "string" to "OK"))
    assertEquals("OK", t5.string)

    val t6 = create<ManyDefaultParams>()
    assertEquals(0, t6.i)
    assertEquals("", t6.s)
    assertEquals(3.14, t6.d)

    return "OK"
}
