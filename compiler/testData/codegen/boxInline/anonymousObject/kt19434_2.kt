// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: NATIVE
// FILE: 1.kt
//WITH_RUNTIME
package test

annotation class FieldAnnotation

inline fun reproduceIssue(crossinline s: () -> String): String {
    val obj = object {
        @field:FieldAnnotation val annotatedField = "O"
        fun method(): String {
            return annotatedField + s()
        }
    }
    val annotatedMethod = obj::class.java.declaredFields.first { it.name == "annotatedField" }
    if (annotatedMethod.annotations.isEmpty()) return "fail: can't find annotated field"
    return obj.method()
}

// FILE: 2.kt
import test.*

fun box(): String {
    return reproduceIssue { "K" }
}