// WITH_STDLIB
package test

class A

abstract class AMutableIterable : MutableIterable<A>

abstract class AMutableIterable2 : MutableIterable<A> by mutableListOf<A>()
