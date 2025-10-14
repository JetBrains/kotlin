// test.AMutableSet
// WITH_STDLIB

package test

class A

abstract class AMutableSet : MutableSet<A> by mutableSetOf<A>()
