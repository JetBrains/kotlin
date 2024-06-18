// ISSUE: KT-68996

annotation class MyAnnotation<T>

typealias FixedAnnotation = MyAnnotation<Int>

class Foo(@FixedAnnotation val inner: Int)
