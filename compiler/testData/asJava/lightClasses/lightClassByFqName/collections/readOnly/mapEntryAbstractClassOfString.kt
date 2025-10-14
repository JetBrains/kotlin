// test.SMapEntry
// WITH_STDLIB

package test

abstract class SMapEntry<VElem> : Map.Entry<String, VElem> by emptyMap<String, VElem>().entries.first()
