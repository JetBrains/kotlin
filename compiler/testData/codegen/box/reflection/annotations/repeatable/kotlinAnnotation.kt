// !LANGUAGE: +RepeatableAnnotations
// TARGET_BACKEND: JVM_IR
// JVM_TARGET: 1.8
// FULL_JDK
// WITH_REFLECT

// In light analysis mode, repeated annotations are not wrapped into the container. This is by design, so that in kapt stubs repeated
// annotations will be visible unwrapped.
// IGNORE_LIGHT_ANALYSIS

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
@Target(CLASS, FUNCTION, PROPERTY)
annotation class A(val value: String)

@A("O") @A("") @A("K")
fun f() {}

@A("O") @A("") @A("") @A("K")
var p = 1

@A("O") @A("K")
class Z

fun box(): String {
    check(::f)
    check(::p)
    check(Z::class)
    return "OK"
}
