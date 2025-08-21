// LANGUAGE: -JvmIndyAllowLambdasWithAnnotations
// LAMBDAS: INDY
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 java/lang/invoke/LambdaMetafactory

@Target(AnnotationTarget.FUNCTION)
public annotation class SomeAnnotation

fun box(): String {
    val a = @SomeAnnotation {}
    val b = @SomeAnnotation fun () {}
    val c = @SomeAnnotation fun Any.() {}

    a()
    b()
    42.c()

    return "OK"
}
