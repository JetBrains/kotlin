package zzz

public class A(val a: Int, val b: Int)

operator inline fun A.component1() = a

operator inline fun A.component2() = b

//SMAP ABSENT