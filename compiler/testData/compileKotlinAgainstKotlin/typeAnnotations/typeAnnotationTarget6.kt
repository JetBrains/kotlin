// KOTLIN_CONFIGURATION_FLAGS: +JVM.EMIT_JVM_TYPE_ANNOTATIONS
// TARGET_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR
// WITH_REFLECT
// FULL_JDK

// FILE: A.kt
// JVM_TARGET: 1.6

import java.lang.reflect.AnnotatedType
import kotlin.reflect.jvm.javaMethod
import kotlin.test.fail

@Target(AnnotationTarget.TYPE)
annotation class TypeAnn

// FILE: B.kt
// JVM_TARGET: 1.8

import java.lang.reflect.AnnotatedType
import kotlin.reflect.jvm.javaMethod
import kotlin.test.fail

class Kotlin {

    fun foo(s: @TypeAnn String) {
    }

    fun foo2(): @TypeAnn String {
        return "OK"
    }
}

fun box(): String {

    checkTypeAnnotation(
        Kotlin::foo.javaMethod!!.annotatedParameterTypes.single(),
        "class java.lang.String",
        "",
        "foo"
    )

    checkTypeAnnotation(Kotlin::foo2.javaMethod!!.annotatedReturnType, "class java.lang.String", "", "foo2")

    return "OK"
}

fun checkTypeAnnotation(
    annotatedType: AnnotatedType,
    type: String,
    annotations: String,
    message: String
) {
    if (annotatedType.annotation() != annotations) fail("check $message (1): ${annotatedType.annotation()} != $annotations")

    if (annotatedType.type.toString() != type) fail("check $message (2): ${annotatedType.type} != $type")
}


fun AnnotatedType.annotation() = annotations.joinToString()
