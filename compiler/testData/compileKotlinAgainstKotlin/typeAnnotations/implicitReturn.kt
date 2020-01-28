// KOTLIN_CONFIGURATION_FLAGS: +JVM.EMIT_JVM_TYPE_ANNOTATIONS
// TARGET_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR
// JVM_TARGET: 1.8
// WITH_REFLECT
// FULL_JDK

// FILE: A.kt

import java.lang.reflect.AnnotatedType
import kotlin.reflect.jvm.javaMethod
import kotlin.test.fail

@Target(AnnotationTarget.TYPE)
annotation class TypeAnn

fun bar(): @TypeAnn String = "OK"

// FILE: B.kt

import java.lang.reflect.AnnotatedType
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.javaField
import kotlin.test.fail

class Kotlin {

    fun foo() = bar()

    @JvmField
    val field = bar()
}

fun box(): String {

    checkTypeAnnotation(
        Kotlin::foo.javaMethod!!.annotatedReturnType,
        "class java.lang.String",
        "@TypeAnn()",
        "foo"
    )

    checkTypeAnnotation(
        Kotlin::field.javaField!!.annotatedType,
        "class java.lang.String",
        "@TypeAnn()",
        "foo"
    )

    return Kotlin().foo()
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
