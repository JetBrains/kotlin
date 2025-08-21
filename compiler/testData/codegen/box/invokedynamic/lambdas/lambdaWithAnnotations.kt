// LANGUAGE: +JvmIndyAllowLambdasWithAnnotations
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_STDLIB
// LAMBDAS: INDY

// CHECK_BYTECODE_TEXT
// 1 java/lang/invoke/LambdaMetafactory

import kotlin.test.assertEquals

@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.TYPE_PARAMETER)
annotation class MyAnnotation

fun higherOrder(action: @MyAnnotation () -> Unit) {
    action()
}

fun box(): String {
    var i = 0
    higherOrder (@MyAnnotation {
        i++
    })

    assertEquals(1, i)

    return "OK"
}
