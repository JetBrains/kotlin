// KOTLIN_CONFIGURATION_FLAGS: +JVM.EMIT_JVM_TYPE_ANNOTATIONS
// TARGET_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JVM_IR
// JVM_TARGET: 1.8
// WITH_REFLECT
// FULL_JDK
package foo

import java.lang.reflect.AnnotatedType
import kotlin.reflect.jvm.javaMethod
import kotlin.test.fail

@Target(AnnotationTarget.TYPE)
annotation class TypeAnn

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.BINARY)
annotation class TypeAnnBinary

class Kotlin {

    fun foo(s: @TypeAnn @TypeAnnBinary String) {
    }

    fun foo2(): @TypeAnn @TypeAnnBinary String {
        return "OK"
    }
}

fun box(): String {

    checkTypeAnnotation(
        Kotlin::foo.javaMethod!!.annotatedParameterTypes.single(),
        "class java.lang.String",
        "@foo.TypeAnn()",
        "foo"
    )

    checkTypeAnnotation(Kotlin::foo2.javaMethod!!.annotatedReturnType, "class java.lang.String", "@foo.TypeAnn()", "foo2")

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
