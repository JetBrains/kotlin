// !DIAGNOSTICS_NUMBER: 1
// !DIAGNOSTICS: ABSTRACT_MEMBER_NOT_IMPLEMENTED
// !MESSAGE_TYPE: TEXT

trait T1
trait T2 : T1
trait T3 : T2
trait T4 : T3


trait A {
    fun f(): T1
}

trait B {
    fun f(): T2 = null!!
}

trait C {
    fun f(): T3
}

abstract class D {
    abstract fun f(): T4
}

class E : A, B, C, D()
