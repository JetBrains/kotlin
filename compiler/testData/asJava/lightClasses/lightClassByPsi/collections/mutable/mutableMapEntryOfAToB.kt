// WITH_STDLIB
package test

class A
class B

abstract class ABMutableMapEntry : MutableMap.MutableEntry<A, B>

abstract class ABMutableMapEntry2 : MutableMap.MutableEntry<A, B> by mutableMapOf<A, B>().entries.first()
