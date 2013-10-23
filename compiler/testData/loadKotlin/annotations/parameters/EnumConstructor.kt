package test

annotation class A
annotation class B

enum class E([A] val x: String, [B] val y: Int)
