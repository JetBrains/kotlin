// WITH_STDLIB
package test

class A
class B

abstract class ABMutableMapEntry : MutableMap.MutableEntry<A, B>

abstract class ABMutableMapEntry2 : MutableMap.MutableEntry<A, B> by mutableMapOf<A, B>().entries.first()

open class ABMutableMapEntry3 : MutableMap.MutableEntry<A, B> {
    override fun setValue(newValue: B): B {
        TODO("Not yet implemented")
    }

    override val key: A
        get() = TODO("Not yet implemented")
    override val value: B
        get() = TODO("Not yet implemented")
}
