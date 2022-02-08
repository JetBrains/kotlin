interface A

class B: A

abstract class Super {
    abstract val a: A
    abstract val b: B
    abstract fun getA(): A
    abstract fun getB(): B
}

class Sub: {
    override val a = B()
    override val b = B()
    override fun getA() = B()
    override fun getB() = B()
}