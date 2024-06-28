package test

annotation class A(vararg val s: String)

@A("1", "2")
class B

@A(*["1", "2"])
class D

@A(s = ["1", "2"])
class E

@A(s = *["1", "2"])
class F

@A(*arrayOf("1", "2"))
class H

@A(s = *arrayOf("1", "2"))
class I

@A(s = arrayOf("1", "2"))
class J
