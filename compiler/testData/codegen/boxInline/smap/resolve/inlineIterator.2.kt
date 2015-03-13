package zzz

public class A(val p: Int)

inline fun A.iterator() = (1..p).iterator()

//SMAP ABSENT