package one

@Repeatable
annotation class AnnoWithArray(val arr: IntArray)

@AnnoWithArray(<expr>[1, 2, 3]</expr>)
class A