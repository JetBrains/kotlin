// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FULL_JDK
// JVM_TARGET: 1.8
// FILE: A.java
import java.util.List;

public class A {
    public static @Anno(1) String test(List<@Anno(2) String> list) {
        return list.get(0);
    }
}

// FILE: Anno.kt

import java.lang.reflect.AnnotatedParameterizedType
import kotlin.test.assertEquals

@Target(AnnotationTarget.TYPE)
annotation class Anno(val value: Int = 0)

fun box(): String {
    val method = A::class.java.declaredMethods.single()
    assertEquals("[@Anno(value=1)]", method.annotatedReturnType.annotations.toList().toString())

    val parameterType = method.parameters.single().annotatedType as AnnotatedParameterizedType
    assertEquals("[@Anno(value=2)]", parameterType.annotatedActualTypeArguments.single().annotations.toList().toString())

    return "OK"
}
