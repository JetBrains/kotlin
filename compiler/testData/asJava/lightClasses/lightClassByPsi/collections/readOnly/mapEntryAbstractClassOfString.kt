// WITH_STDLIB
package test

abstract class SMapEntry<VElem> : Map.Entry<String, VElem>

abstract class SMapEntry2<VElem> : Map.Entry<String, VElem> by emptyMap<String, VElem>().entries.first()

open class SMapEntry3<VElem> : Map.Entry<String, VElem> {
    override val key: String
        get() = TODO("Not yet implemented")
    override val value: VElem
        get() = TODO("Not yet implemented")
}

// LIGHT_ELEMENTS_NO_DECLARATION: SMapEntry.class[setValue], SMapEntry2.class[setValue], SMapEntry3.class[setValue]