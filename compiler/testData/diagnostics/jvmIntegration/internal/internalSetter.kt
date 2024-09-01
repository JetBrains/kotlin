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
    <!INVISIBLE_SETTER!>A().v<!> = 4
    <!INVISIBLE_SETTER!>B<Int>().v<!> = 4
    <!INVISIBLE_SETTER!>C().v<!> = 4
    <!INVISIBLE_SETTER!>D<Int>().v<!> = 4
}
