package test

interface Some {
    fun test() = 10
}

abstract class My<T : Some> {
    open class T

    abstract val x: T

    // Wouldn't work for the inner class T
    fun foo(arg: T) = arg.test() + x.test()

    abstract val y: My.T

    abstract val z: test.My.T

    // Would work for the type parameter T
    fun boo() = y.<!UNRESOLVED_REFERENCE!>test<!>() + z.<!UNRESOLVED_REFERENCE!>test<!>()

    class Some : T()
}

abstract class Your<T : Some> : <!SUPERTYPE_NOT_A_CLASS_OR_INTERFACE!>T<!>
