// LANGUAGE: +RepeatableAnnotations
// TARGET_BACKEND: JVM_IR
// JVM_TARGET: 1.8
// FULL_JDK
// WITH_REFLECT

// Test failed to run to completion. Reason: 'Instrumentation run failed due to 'Native crash''. Check device logcat for details
// Because TYPE_USE is not avaiable before API level 26
// IGNORE_BACKEND: ANDROID

import kotlin.annotation.AnnotationTarget.*
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.full.hasAnnotation

fun check(element: KAnnotatedElement) {
    if (!element.hasAnnotation<A>()) throw AssertionError("Fail hasAnnotation $element")

    val find = element.findAnnotation<A>()
    if (find == null || find.value != "O") throw AssertionError("Fail findAnnotation $element: $find")

    val all = element.annotations
    val findAll = element.findAnnotations<A>()
    if (all != findAll) throw AssertionError("Fail findAnnotations $element: $all != $findAll")

    if (all.any { it !is A })
        throw AssertionError("Fail 1 $element: $all")
    if (all.fold("") { acc, it -> acc + (it as A).value } != "OK")
        throw AssertionError("Fail 2 $element: $all")
}

@Repeatable
@Target(CLASS, FUNCTION, PROPERTY, TYPE)
annotation class A(val value: String)

fun g(): @A("O") @A("K") @A("") Unit {}

fun box(): String {
    check(::g.returnType)
    return "OK"
}
