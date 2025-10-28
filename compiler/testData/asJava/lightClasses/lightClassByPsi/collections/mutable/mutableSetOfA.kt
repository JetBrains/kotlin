// WITH_STDLIB
package test

class A

abstract class AMutableSet : MutableSet<A>

abstract class AMutableSet2 : MutableSet<A> by mutableSetOf<A>()
