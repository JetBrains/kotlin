// ISSUE: KT-69766
// MODULE: lib
class A {
    var v: Int = 0
        internal set
}

class B<T> {
    var v: T = null!!
        internal set
}

interface Some {
    val v: Int
}

abstract class Base {
    var v: Int = 1
        internal set
}

class C : Base(), Some

interface Other<T> {
    val v: T
}

abstract class GenericBase<T> {
    var v: T = null!!
        internal set
}

class D<T> : GenericBase<T>(), Other<T>

// MODULE: main(lib)
fun test() {
    A().<!INVISIBLE_SETTER!>v<!> = 4
    B<Int>().<!INVISIBLE_SETTER!>v<!> = 4
    C().<!INVISIBLE_SETTER!>v<!> = 4
    D<Int>().<!INVISIBLE_SETTER!>v<!> = 4
}
