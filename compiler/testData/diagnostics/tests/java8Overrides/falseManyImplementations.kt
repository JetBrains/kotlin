// FIR_IDENTICAL
abstract class ClassEmpty {
    abstract fun foo()
}

interface BaseEmpty {
    fun foo()
}

interface BaseDefault {
    fun foo() {}
}

abstract class ClassEmpty_BaseEmpty_BaseDefault : ClassEmpty(), BaseEmpty, BaseDefault
