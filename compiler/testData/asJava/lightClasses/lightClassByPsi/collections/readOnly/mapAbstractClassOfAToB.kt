// WITH_STDLIB
package test

class A
class B

abstract class ABMap : Map<A, B>

abstract class ABMap2 : Map<A, B> by emptyMap<A, B>()
