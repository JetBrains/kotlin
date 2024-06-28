// LAMBDAS: INDY
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 0 java/lang/invoke/LambdaMetafactory

import kotlin.jvm.internal.Lambda

@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
public annotation class SomeAnnotation

fun box(): String {
    val a = @SomeAnnotation {}
    val b = @SomeAnnotation fun () {}
    val c = @SomeAnnotation fun Any.() {}

    return "OK"
}
