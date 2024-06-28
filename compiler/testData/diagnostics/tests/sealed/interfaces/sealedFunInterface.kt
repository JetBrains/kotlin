// FIR_IDENTICAL
<!UNSUPPORTED_SEALED_FUN_INTERFACE!>sealed<!> fun interface A { // error
    fun foo()
}

sealed interface Base {
    <!UNSUPPORTED_SEALED_FUN_INTERFACE!>sealed<!> fun interface Derived : Base {  // error
        fun foo()
    }
}

sealed interface IBase {
    fun interface IDerived : IBase { // OK
        fun foo()
    }
}
