// WITH_STDLIB
package test

class A

abstract class AMutableList : MutableList<A>

abstract class AMutableList2 : MutableList<A> by mutableListOf<A>()
