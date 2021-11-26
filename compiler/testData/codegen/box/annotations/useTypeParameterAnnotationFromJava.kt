// TARGET_BACKEND: JVM
// WITH_STDLIB
// FULL_JDK
// JVM_TARGET: 1.8
// FILE: A.java
import java.util.List;

public class A<@Anno(1) T> {}

// FILE: Anno.kt

import kotlin.test.assertTrue

@Target(AnnotationTarget.TYPE_PARAMETER)
annotation class Anno(val value: Int = 0)

fun box(): String {
    val typeParameter = A::class.java.typeParameters.single()
    val parametertoString = typeParameter.annotations.toList().toString()
    assertTrue("\\[@Anno\\((value=)?1\\)\\]".toRegex().matches(parametertoString), parametertoString)
    return "OK"
}
