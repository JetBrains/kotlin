// WITH_STDLIB
package test

abstract class CMapEntry<KElem, VElem> : Map.Entry<KElem, VElem>

abstract class CMapEntry2<KElem, VElem> : Map.Entry<KElem, VElem> by emptyMap<KElem, VElem>().entries.first()
