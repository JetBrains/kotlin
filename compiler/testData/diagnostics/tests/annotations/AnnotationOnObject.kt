package test

annotation class A(val a: Int = 12, val b: String = "Test", val c: String)

@A(a = 12, c = "Hello")
object SomeObject
