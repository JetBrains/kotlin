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
    assert((@SomeAnnotation {}) is Lambda<*>)
    assert((@SomeAnnotation fun () {}) is Lambda<*>)
    assert((@SomeAnnotation fun Any.() {}) is Lambda<*>)

    return "OK"
}
