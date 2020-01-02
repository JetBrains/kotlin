open class A(val x: Any)

class B : A(this::class)
