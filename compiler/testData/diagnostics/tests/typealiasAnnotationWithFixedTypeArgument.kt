// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// ISSUE: KT-68996
// LATEST_LV_DIFFERENCE

annotation class MyAnnotation<T>

typealias FixedAnnotation = MyAnnotation<Int>

class Foo(@FixedAnnotation val inner: Int)
