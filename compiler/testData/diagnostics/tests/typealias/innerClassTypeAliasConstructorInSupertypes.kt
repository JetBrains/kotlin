// !WITH_NEW_INFERENCE
package test

typealias OI = Outer.Inner

class Outer {
    open inner class Inner

    inner class Test : OI()
}


typealias GI<T> = Generic<T>.Inner
typealias GIInt = Generic<Int>.Inner
typealias GIStar = Generic<*>.Inner
typealias GG<T1, T2> = Generic<T1>.Generic<T2>
typealias GIntG<T2> = Generic<Int>.Generic<T2>
typealias GGInt<T1> = Generic<T1>.Generic<Int>

class Generic<T1> {
    open inner class Inner
    open inner class Generic<T2>

    inner class Test1 : GI<T1>()
    inner class Test2 : <!DEBUG_INFO_UNRESOLVED_WITH_TARGET{NI}, TYPE_MISMATCH{OI}, UNRESOLVED_REFERENCE_WRONG_RECEIVER{NI}!>GIInt<!>()
    inner class Test3 : GIStar()
    inner class Test3a : test.Generic<*>.Inner()

    inner class Test4<T2> : GG<T1, T2>()
    inner class Test5 : GG<T1, Int>()
    inner class Test6 : <!DEBUG_INFO_UNRESOLVED_WITH_TARGET{NI}, TYPE_MISMATCH{OI}, UNRESOLVED_REFERENCE_WRONG_RECEIVER{NI}!>GG<!><Int, T1>()
    inner class Test7 : <!DEBUG_INFO_UNRESOLVED_WITH_TARGET{NI}, TYPE_MISMATCH{OI}, UNRESOLVED_REFERENCE_WRONG_RECEIVER{NI}!>GG<!><Int, Int>()
    inner class Test8 : <!DEBUG_INFO_UNRESOLVED_WITH_TARGET{NI}, TYPE_MISMATCH{OI}, UNRESOLVED_REFERENCE_WRONG_RECEIVER{NI}!>GIntG<!><Int>()
    inner class Test9 : <!DEBUG_INFO_UNRESOLVED_WITH_TARGET{NI}, TYPE_MISMATCH{OI}, UNRESOLVED_REFERENCE_WRONG_RECEIVER{NI}!>GGInt<!><Int>()
    inner class Test10 : GGInt<T1>()

    inner class Test11 : GG<T1, Int> {
        constructor() : super()
    }
}
