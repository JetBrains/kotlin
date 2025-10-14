// test.SMutableMapEntry
// WITH_STDLIB

package test

abstract class SMutableMapEntry<VElem> : MutableMap.MutableEntry<String, VElem> by mutableMapOf<String, VElem>().entries.first()
