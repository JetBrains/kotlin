package test

interface Some

abstract class My<T : Some> {
    inner class T

    abstract val x: T

    abstract fun foo(arg: T)

    abstract val y: <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>My.T<!>

    abstract val z: <!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>test.My.T<!>

    class Some : <!UNRESOLVED_REFERENCE{LT}!><!SUPERTYPE_NOT_A_CLASS_OR_INTERFACE, UNRESOLVED_REFERENCE{PSI}!>T<!>()<!>
}

abstract class Your<T : Some> : <!SUPERTYPE_NOT_A_CLASS_OR_INTERFACE!>T<!>
