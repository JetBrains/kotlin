package zzz

public class A(val p: Int)

operator inline fun A.iterator() = (1..p).iterator()

//SMAP ABSENT