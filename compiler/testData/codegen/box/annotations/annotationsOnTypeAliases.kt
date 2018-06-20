// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_RUNTIME
// FULL_JDK

import kotlin.annotation.AnnotationTarget.*
import kotlin.annotation.AnnotationRetention.*
import java.lang.Class

@Target(TYPEALIAS)
@Retention(RUNTIME)
annotation class Ann(val x: Int)

@Ann(2)
typealias TA = Any

fun Class<*>.assertHasDeclaredMethodWithAnn() {
    if (!declaredMethods.any { it.isSynthetic && it.getAnnotation(Ann::class.java) != null }) {
        throw java.lang.AssertionError("Class ${this.simpleName} has no declared method with annotation @Ann")
    }
}

fun box(): String {
    Class.forName("AnnotationsOnTypeAliasesKt").assertHasDeclaredMethodWithAnn()

    return "OK"
}