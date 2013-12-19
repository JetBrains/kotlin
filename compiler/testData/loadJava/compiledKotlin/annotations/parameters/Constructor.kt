package test

annotation class A
annotation class B

class Class([A] val x: Int, [B] y: String)
