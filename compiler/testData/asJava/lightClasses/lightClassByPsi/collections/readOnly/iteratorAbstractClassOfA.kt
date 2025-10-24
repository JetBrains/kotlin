// WITH_STDLIB
package test

class A

abstract class AIterator : Iterator<A>

abstract class AIterator2 : Iterator<A> by emptyList<A>().iterator()
