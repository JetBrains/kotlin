// WITH_STDLIB
package test

class A

abstract class ACollection : Collection<A>

abstract class ACollection2 : Collection<A> by emptyList<A>()
