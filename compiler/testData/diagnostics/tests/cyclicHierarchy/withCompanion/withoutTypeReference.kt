// FIR_IDENTICAL
// see https://youtrack.jetbrains.com/issue/KT-21515

abstract class DerivedAbstract : C.Base()

class Data

open class C {
    open class Base {
        open fun m() {}
    }

    val field = Data()

    companion object : DerivedAbstract() {
        override fun m() {}
    }
}