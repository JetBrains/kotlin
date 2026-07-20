// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: MyAnnotation.java
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Target(java.lang.annotation.ElementType.TYPE)
public @interface MyAnnotation {
    String value() default "";
    int count() default 0;
    Class<?> type() default Object.class;
    String[] tags() default {};
}

// FILE: box.kt
import kotlin.reflect.jvm.javaType
import kotlin.test.assertEquals

fun box(): String {
    val ctor = MyAnnotation::class.constructors.single()

    try {
        assertEquals(
            "int count, class [Ljava.lang.String; tags, java.lang.Class<?> type, class java.lang.String value",
            ctor.parameters.sortedBy { it.name }.map { it.type.javaType.toString() + " " + it.name }.joinToString(),
        )
        return "OK"
    } catch (e: Throwable) {
        if (Class.forName("kotlin.reflect.jvm.internal.SystemPropertiesKt").getMethod("getUseK1Implementation").invoke(null) == true) {
            // KT-87371
            return "OK"
        } else {
            throw e
        }
    }
}
