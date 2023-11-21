// FIR_IDENTICAL
// TARGET_BACKEND: JVM_IR
// ISSUE: KT-68996
// DUMP_IR
// JVM_ABI_K1_K2_DIFF: K2 stores annotations in metadata (KT-57919).

annotation class MyAnnotation<T>

typealias FixedAnnotation = MyAnnotation<Int>

class Foo(@FixedAnnotation val inner: Int)

fun box() = "OK"
