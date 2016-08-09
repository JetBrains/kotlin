open class A(val x: Any)

class B : A(<!NO_THIS!>this<!>::class)
