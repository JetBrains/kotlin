// WITH_STDLIB
package test

abstract class SMapEntry<VElem> : Map.Entry<String, VElem>

abstract class SMapEntry2<VElem> : Map.Entry<String, VElem> by emptyMap<String, VElem>().entries.first()
