// WITH_STDLIB
package test

class A
class B

abstract class ABMapEntry : Map.Entry<A, B>

abstract class ABMapEntry2 : Map.Entry<A, B> by emptyMap<A, B>().entries.first()

open class ABMapEntry3 : Map.Entry<A, B> {
    override val key: A
        get() = TODO("Not yet implemented")
    override val value: B
        get() = TODO("Not yet implemented")
}

// LIGHT_ELEMENTS_NO_DECLARATION: ABMapEntry.class[setValue], ABMapEntry2.class[setValue], ABMapEntry3.class[setValue]