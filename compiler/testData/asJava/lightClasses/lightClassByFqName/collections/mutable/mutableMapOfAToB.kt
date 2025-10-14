// test.ABMutableMap
// WITH_STDLIB

package test

class A
class B

abstract class ABMutableMap : MutableMap<A, B> by mutableMapOf<A, B>()
