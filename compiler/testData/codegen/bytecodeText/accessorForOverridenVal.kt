// IGNORE_BACKEND: JVM_IR
package b

abstract class B {
    open val propWithFinal: Int
        get() = 1

    open val propWithNonFinal: Int
        get() = 2
}

abstract class Base: B() {
    override final val propWithFinal: Int = 3
    override val propWithNonFinal: Int = 4

    fun fooFinal() = this.propWithFinal
    fun fooNonFinal() = this.propWithNonFinal
}

// 2 GETFIELD b/Base.propWithFinal : I
// 1 INVOKEVIRTUAL b/Base.getPropWithNonFinal \(\)I