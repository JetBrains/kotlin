// FIR_IDENTICAL
class X {
  <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>val x : Int<!>
}

open class Y() {
  val x : Int = 2
}

class Y1 {
  val x : Int get() = 1
}

class Z : Y() {
}

//KT-650 Prohibit creating class without constructor.

class MyIterable<T> : Iterable<T>
{
    override fun iterator(): Iterator<T>  = MyIterator()

    inner class MyIterator : Iterator<T>
    {
        override fun hasNext(): Boolean = false
        override fun next(): T {
            throw UnsupportedOperationException()
        }
    }
}