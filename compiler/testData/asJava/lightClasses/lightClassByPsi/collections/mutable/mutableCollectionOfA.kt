// WITH_STDLIB
package test

class A

abstract class AMutableCollection : MutableCollection<A>

abstract class AMutableCollection2 : MutableCollection<A> by mutableListOf<A>()
