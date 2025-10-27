// WITH_STDLIB
package test

class A

abstract class ASet : Set<A>

abstract class ASet2 : Set<A> by emptySet<A>()
