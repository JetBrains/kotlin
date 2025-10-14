// test.AMutableIterable
// WITH_STDLIB

package test

class A

abstract class AMutableIterable : MutableIterable<A> by mutableListOf<A>()
