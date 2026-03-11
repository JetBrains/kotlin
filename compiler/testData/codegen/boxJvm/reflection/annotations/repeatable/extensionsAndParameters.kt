// TARGET_BACKEND: JVM_IR
// JVM_TARGET: 1.8
// FULL_JDK
// WITH_REFLECT

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
@Target(FUNCTION, PROPERTY, PROPERTY_GETTER, PROPERTY_SETTER, VALUE_PARAMETER)
annotation class A(val value: String)

@A("ext") @A("Fun")
fun @receiver:[A("extFun") A("Receiver")] String.extensionFunction(@A("par") @A("am") x: Any) {}

@A("ext") @A("Prop")
@get:[A("ext") A("PropGet")]
@set:[A("ext") A("PropSet")]
@setparam:[A("ext") A("PropSetparam")]
var @receiver:[A("extProp") A("Receiver")] String.extensionProperty: String
    get() = this
    set(value) {}

fun box(): String {
    val extFun = String::extensionFunction
    check(extFun, "extFun")
    check(extFun.parameters[0], "extFunReceiver")
    check(extFun.parameters[1], "param")

    val extProp = String::extensionProperty
    check(extProp, "extProp")
    check(extProp.parameters.single(), "extPropReceiver")
    check(extProp.getter, "extPropGet")
    check(extProp.getter.parameters.single(), "extPropReceiver")
    check(extProp.setter, "extPropSet")
    check(extProp.setter.parameters[0], "extPropReceiver")
    check(extProp.setter.parameters[1], "extPropSetparam")

    return "OK"
}
