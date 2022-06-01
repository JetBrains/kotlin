// !LANGUAGE: +RepeatableAnnotations
// TARGET_BACKEND: JVM_IR
// JVM_TARGET: 1.8
// FULL_JDK
// WITH_REFLECT

// Android doesn't have @Repeatable before API level 24, so findAnnotations can't unpack repeatable annotations.
// IGNORE_BACKEND: ANDROID

// In light analysis mode, repeated annotations are not wrapped into the container. This is by design, so that in kapt stubs repeated
// annotations will be visible unwrapped.
// IGNORE_LIGHT_ANALYSIS

// FILE: A.kt
@java.lang.annotation.Repeatable(A.Container::class)
annotation class A(val value: String) {
    annotation class Container(val value: Array<A>)
}

// FILE: kt49335.kt
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.full.hasAnnotation

@A("O")
@A("")
@A("K")
fun f() {}

fun box(): String {
    val element = ::f
    if (element.hasAnnotation<A>()) return "Fail hasAnnotation $element"
    val find = element.findAnnotation<A>()
    if (find != null) return "Fail findAnnotation $element: $find"

    val all = (element.annotations.single() as A.Container).value.asList()
    val findAll = element.findAnnotations<A>()
    if (all != findAll) throw AssertionError("Fail findAnnotations $element: $all != $findAll")

    return all.fold("") { acc, it -> acc + it.value }
}
