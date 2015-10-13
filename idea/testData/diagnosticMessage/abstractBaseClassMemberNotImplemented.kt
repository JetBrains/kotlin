// !DIAGNOSTICS_NUMBER: 1
// !DIAGNOSTICS: ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED
// !MESSAGE_TYPE: TEXT

interface T1
interface T2 : T1
interface T3 : T2
interface T4 : T3


interface A {
    fun f(): T1
}

interface B {
    fun f(): T2 = null!!
}

interface C {
    fun f(): T3
}

abstract class D {
    abstract fun f(): T4
}

class E : A, B, C, D()
