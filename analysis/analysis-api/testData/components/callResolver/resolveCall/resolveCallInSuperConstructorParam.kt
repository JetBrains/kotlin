infix fun <A, B> A.to(other: B) = this

open class A<T>(x: T)

class B : A(<expr>1 to 2</expr>)
