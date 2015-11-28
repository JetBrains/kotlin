package test

class A
class B

inline fun <reified T> Any?.foo(): T? = this as? T
