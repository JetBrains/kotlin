// test.AMutableCollection
// WITH_STDLIB

package test

class A

abstract class AMutableCollection : MutableCollection<A> by mutableListOf<A>()
