// test.ABMapEntry
// WITH_STDLIB

package test

class A
class B

abstract class ABMapEntry : Map.Entry<A, B> by emptyMap<A, B>().entries.first()
