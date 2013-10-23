package test

annotation class A
annotation class B
annotation class C
annotation class D

fun foo([A B] x: Int, [A C] y: Double, [B C D] z: String) {}

fun bar([A B C D] x: Int) {}
