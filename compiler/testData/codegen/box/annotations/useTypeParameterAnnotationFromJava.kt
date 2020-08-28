// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FULL_JDK
// JVM_TARGET: 1.8
// FILE: A.java
import java.util.List;

public class A<@Anno(1) T> {}

// FILE: Anno.kt

import kotlin.test.assertEquals

@Target(AnnotationTarget.TYPE_PARAMETER)
annotation class Anno(val value: Int = 0)

fun box(): String {
    val typeParameter = A::class.java.typeParameters.single()
    assertEquals("[@Anno(value=1)]", typeParameter.annotations.toList().toString())

    return "OK"
}
