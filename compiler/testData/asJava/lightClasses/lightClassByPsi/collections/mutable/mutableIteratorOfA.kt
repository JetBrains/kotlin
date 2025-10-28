// WITH_STDLIB
package test

class A

abstract class AMutableIterator : MutableIterator<A>

abstract class AMutableIterator2 : MutableIterator<A> by mutableListOf<A>().iterator()
