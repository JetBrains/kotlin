package zzz

public class A(val a: Int, val b: Int)

inline fun A.component1() = a

inline fun A.component2() = b

//SMAP ABSENT