// test.AMutableIterator
// WITH_STDLIB

package test

class A

abstract class AMutableIterator : MutableIterator<A> by mutableListOf<A>().iterator()
