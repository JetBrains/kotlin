// IGNORE_BACKEND: JVM_IR
inline fun <reified T> Any?.foo() = this as T?

inline fun <reified Y> Any?.foo2() = foo<Y?>()

inline fun <reified Z> Any?.foo3() = foo2<Z>()

inline fun <reified X> Any?.foo4() = foo2<X?>()

inline fun <reified A> Any?.foo5() = foo<A>()

// 1 LDC "T\?"
// 1 LDC "Y\?"
// 1 LDC "Z\?"
// 1 LDC "X\?"
// 1 LDC "A\?"