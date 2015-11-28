package test

class A
class B

inline fun <reified T> Any?.foo() = this is T

inline fun <reified Y> Any?.foo2() = foo<Y?>()

inline fun <reified Z> Any?.foo3() = foo2<Z>()
