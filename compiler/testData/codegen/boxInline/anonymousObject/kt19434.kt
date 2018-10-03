// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: NATIVE
// FILE: 1.kt
//WITH_RUNTIME
package test

annotation class MethodAnnotation

inline fun reproduceIssue(crossinline s: () -> String): String {
    val obj = object {
        @MethodAnnotation fun annotatedMethod(): String {
            return s()
        }
    }
    val annotatedMethod = obj::class.java.declaredMethods.first { it.name == "annotatedMethod" }
    if (annotatedMethod.annotations.isEmpty()) return "fail: can't find annotated method"
    return obj.annotatedMethod()
}

// FILE: 2.kt
import test.*

fun box(): String {
    return reproduceIssue { "OK" }
}