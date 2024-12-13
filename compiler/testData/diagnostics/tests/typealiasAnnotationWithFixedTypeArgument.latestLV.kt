// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// ISSUE: KT-68996
// LATEST_LV_DIFFERENCE

annotation class MyAnnotation<T>

typealias FixedAnnotation = MyAnnotation<Int>

class Foo(<!ANNOTATION_WILL_BE_APPLIED_ALSO_TO_PROPERTY_OR_FIELD("property")!>@FixedAnnotation<!> val inner: Int)
