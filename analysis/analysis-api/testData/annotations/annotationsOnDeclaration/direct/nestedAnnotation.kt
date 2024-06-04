annotation class B(val i: Int)
annotation class A(val b: B)

@A(b = B(i = 42))
class Te<caret>st