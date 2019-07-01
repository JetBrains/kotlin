// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM
// FILE: A.kt
package a

import kotlin.annotation.AnnotationTarget.*
import kotlin.annotation.AnnotationRetention.*
import java.lang.Class

@Target(TYPEALIAS)
@Retention(RUNTIME)
annotation class Ann(val x: Int)

@Ann(2)
typealias TA = Any

// FILE: B.kt
import a.Ann

fun Class<*>.assertHasDeclaredMethodWithAnn() {
    if (!declaredMethods.any { it.isSynthetic && it.getAnnotation(Ann::class.java) != null }) {
        throw java.lang.AssertionError("Class ${this.simpleName} has no declared method with annotation @Ann")
    }
}

fun box(): String {
    Class.forName("a.AKt").assertHasDeclaredMethodWithAnn()

    return "OK"
}