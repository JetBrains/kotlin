// FIR_IDENTICAL
// TARGET_BACKEND: JVM_IR
// ISSUE: KT-68996
// DUMP_IR

annotation class MyAnnotation<T>

typealias FixedAnnotation = MyAnnotation<Int>

class Foo(@FixedAnnotation val inner: Int)

fun box() = "OK"
