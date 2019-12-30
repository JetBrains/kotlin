// KOTLIN_CONFIGURATION_FLAGS: +JVM.EMIT_JVM_TYPE_ANNOTATIONS
// TARGET_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JVM_IR
// JVM_TARGET: 1.8
// WITH_REFLECT
// FULL_JDK

// FILE: ImplicitReturn.java

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


public class ImplicitReturn {

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE_USE)
    public @ interface TypeAnn {}

    @ImplicitReturn.TypeAnn
    public String bar() {
        return "OK";
    }
}


// FILE: Kotlin.kt

import java.lang.reflect.AnnotatedType
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.javaField
import kotlin.test.fail

class Kotlin {

    fun foo() = ImplicitReturn().bar()

    @JvmField
    val field = ImplicitReturn().bar()
}

fun box(): String {

    checkTypeAnnotation(
        Kotlin::foo.javaMethod!!.annotatedReturnType,
        "class java.lang.String",
        "@ImplicitReturn\$TypeAnn()",
        "foo"
    )

    checkTypeAnnotation(
        Kotlin::field.javaField!!.annotatedType,
        "class java.lang.String",
        "@ImplicitReturn\$TypeAnn()",
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
