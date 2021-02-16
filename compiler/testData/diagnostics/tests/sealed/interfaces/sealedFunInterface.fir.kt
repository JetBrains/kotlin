sealed fun interface A { // error
    fun foo()
}

sealed interface Base {
    sealed fun interface Derived : Base {  // error
        fun foo()
    }
}

sealed interface IBase {
    fun interface IDerived : IBase { // OK
        fun foo()
    }
}
