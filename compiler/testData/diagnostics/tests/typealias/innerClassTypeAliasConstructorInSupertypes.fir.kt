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
    inner class Test2 : <!UNRESOLVED_REFERENCE!>GIInt<!>()
    inner class Test3 : <!CONSTRUCTOR_OR_SUPERTYPE_ON_TYPEALIAS_WITH_TYPE_PROJECTION_WARNING!>GIStar<!>()
    inner class Test3a : test.Generic<<!PROJECTION_IN_IMMEDIATE_ARGUMENT_TO_SUPERTYPE!>*<!>>.Inner()

    inner class Test4<T2> : GG<T1, T2>()
    inner class Test5 : GG<T1, Int>()
    inner class Test6 : <!UNRESOLVED_REFERENCE!>GG<Int, T1><!>()
    inner class Test7 : <!UNRESOLVED_REFERENCE!>GG<Int, Int><!>()
    inner class Test8 : <!UNRESOLVED_REFERENCE!>GIntG<Int><!>()
    inner class Test9 : <!UNRESOLVED_REFERENCE!>GGInt<Int><!>()
    inner class Test10 : GGInt<T1>()

    inner class Test11 : GG<T1, Int> {
        constructor() : super()
    }
}
