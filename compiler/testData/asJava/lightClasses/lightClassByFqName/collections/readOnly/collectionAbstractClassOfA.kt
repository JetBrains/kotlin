// test.ACollection
// WITH_STDLIB

package test

class A

abstract class ACollection : Collection<A> by emptyList<A>()
