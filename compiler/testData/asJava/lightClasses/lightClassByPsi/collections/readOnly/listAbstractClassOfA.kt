// WITH_STDLIB
package test

class A

abstract class AList : List<A>

abstract class AList2 : List<A> by emptyList<A>()
