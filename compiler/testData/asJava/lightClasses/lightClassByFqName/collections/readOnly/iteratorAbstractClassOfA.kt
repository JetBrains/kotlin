// test.AIterator
// WITH_STDLIB

package test

class A

abstract class AIterator : Iterator<A> by emptyList<A>().iterator()
