// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: SampleAnnotation.java
import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SampleAnnotation {
    String  text()    default "";
    int     count()   default 0;
    Class<?> type()  default Object.class;
    String[] tags()  default {};
    boolean flag()   default false;
}

// FILE: box.kt
// Tests that KType.javaType on Java annotation element parameter types is accessible
// and returns the correct Java types.

import kotlin.reflect.jvm.javaType
import kotlin.reflect.full.*
import kotlin.test.*

fun box(): String {
    val ctor = SampleAnnotation::class.constructors.single()

    val paramsByName = ctor.parameters.associateBy { it.name }

    // Each element type must be accessible via javaType without throwing
    val expectedJavaTypes = mapOf(
        "text"  to "java.lang.String",
        "count" to "int",
        "type"  to "java.lang.Class",   // Class<?> erases to Class
        "tags"  to "[Ljava.lang.String;",
        "flag"  to "boolean"
    )

    for ((name, expectedType) in expectedJavaTypes) {
        val param = paramsByName[name]
            ?: return "Fail: annotation element '$name' not found in constructor parameters"

        val javaType = runCatching { param.type.javaType.typeName }
            .getOrElse { return "Fail: javaType for '$name' threw ${it::class.simpleName}: ${it.message}" }

        assertTrue(javaType.startsWith(expectedType) || javaType == expectedType,
            "Annotation element '$name': expected javaType starting with '$expectedType', got: $javaType")
    }

    return "OK"
}
