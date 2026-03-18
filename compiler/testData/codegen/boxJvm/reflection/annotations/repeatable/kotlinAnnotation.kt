// LANGUAGE: +RepeatableAnnotations
// TARGET_BACKEND: JVM_IR
// JVM_TARGET: 1.8
// FULL_JDK
// WITH_REFLECT
// JVM_ABI_K1_K2_DIFF

import kotlin.annotation.AnnotationTarget.*
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.full.hasAnnotation

fun check(element: KAnnotatedElement, expected: String) {
    if (!element.hasAnnotation<A>()) throw AssertionError("Fail hasAnnotation $element")

    val find = element.findAnnotation<A>()
    if (find == null || !expected.startsWith(find.value)) throw AssertionError("Fail findAnnotation $element: $find")

    val all = element.annotations
    val findAll = element.findAnnotations<A>()
    if (all != findAll) throw AssertionError("Fail findAnnotations $element: $all != $findAll")

    if (all.any { it !is A })
        throw AssertionError("Fail 1 $element: $all")
    if (all.fold("") { acc, it -> acc + (it as A).value } != expected)
        throw AssertionError("Fail 2 $element: expected $expected, actual $all")
}

@Repeatable
@Target(CLASS, FUNCTION, PROPERTY, PROPERTY_GETTER, PROPERTY_SETTER, VALUE_PARAMETER)
annotation class A(val value: String)

@A("f") @A("un") @A("")
fun f(@A("par") @A("am") x: Any) {}

@A("p") @A("rop") @A("erty")
@get:[A("g") A("e") A("t")]
@set:[A("se") A("") A("t")]
@setparam:[A("set") A("param")]
var p = 1

@A("c") @A("lass")
class Z

fun box(): String {
    check(::f, "fun")
    check(::f.parameters.single(), "param")
    check(::p, "property")
    check(::p.getter, "get")
    check(::p.setter, "set")
    check(::p.setter.parameters.single(), "setparam")
    check(Z::class, "class")

    return "OK"
}
