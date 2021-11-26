// TARGET_BACKEND: JVM
// WITH_STDLIB
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
import kotlin.test.assertTrue

@Target(AnnotationTarget.TYPE)
annotation class Anno(val value: Int = 0)

fun box(): String {
    val method = A::class.java.declaredMethods.single()
    val methodToString = method.annotatedReturnType.annotations.toList().toString()
    assertTrue("\\[@Anno\\((value=)?1\\)\\]".toRegex().matches(methodToString), methodToString)
    
    val parameterType = method.parameters.single().annotatedType as AnnotatedParameterizedType
    val parameterToString = parameterType.annotatedActualTypeArguments.single().annotations.toList().toString()
    assertTrue("\\[@Anno\\((value=)?2\\)\\]".toRegex().matches(parameterToString), parameterToString)

    return "OK"
}
