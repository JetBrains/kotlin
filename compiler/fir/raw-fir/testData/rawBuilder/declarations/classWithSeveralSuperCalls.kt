open class A(x: Int)

open class B(y: Int)

class WithPrimaryConstructor(x: Int) : A(x + 1), B(x + 2)

class WithImplicitConstructor : A(1 + 2), B(3 + 4)
