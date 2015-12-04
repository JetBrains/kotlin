package test

class A
class B

inline fun <reified T> Any?.foo(): T = this as T

inline fun <reified Y> Any?.foo2(): Y? = foo<Y?>()

inline fun <reified Z> Any?.foo3(): Z? = foo2<Z>()
