// ISSUE: KT-68996

annotation class MyAnnotation<T>

typealias FixedAnnotation = MyAnnotation<Int>

class Foo(@<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>FixedAnnotation<!> val inner: Int)
