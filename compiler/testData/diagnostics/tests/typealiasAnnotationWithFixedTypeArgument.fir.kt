// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-68996

annotation class MyAnnotation<T>

typealias FixedAnnotation = MyAnnotation<Int>

class Foo(<!ANNOTATION_WILL_BE_APPLIED_ALSO_TO_PROPERTY_OR_FIELD("property")!>@FixedAnnotation<!> val inner: Int)
