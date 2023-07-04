// ORIGINAL: /compiler/testData/diagnostics/tests/sealed/interfaces/sealedFunInterface.fir.kt
// WITH_STDLIB
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


fun box() = "OK".also { foo() }
