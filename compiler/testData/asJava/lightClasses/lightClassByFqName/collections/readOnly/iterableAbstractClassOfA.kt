// test.AIterable
// WITH_STDLIB

package test

class A

abstract class AIterable : Iterable<A> by emptyList<A>()
