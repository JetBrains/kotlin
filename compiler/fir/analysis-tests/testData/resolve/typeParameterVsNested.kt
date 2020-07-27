package test

interface Some

abstract class My<T : Some> {
    inner class T

    abstract val x: T

    abstract fun foo(arg: T)

    abstract val y: <!OTHER_ERROR!>My.T<!>

    abstract val z: <!OTHER_ERROR!>test.My.T<!>

    class Some : <!UNRESOLVED_REFERENCE!>T<!>()
}

abstract class Your<T : Some> : <!OTHER_ERROR!>T<!>