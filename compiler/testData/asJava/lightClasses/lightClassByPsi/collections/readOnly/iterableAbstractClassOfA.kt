// WITH_STDLIB
package test

class A

abstract class AIterable : Iterable<A>

abstract class AIterable2 : Iterable<A> by emptyList<A>()
