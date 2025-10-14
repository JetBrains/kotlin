// test.AMutableList
// WITH_STDLIB

package test

class A

abstract class AMutableList : MutableList<A> by mutableListOf<A>()
