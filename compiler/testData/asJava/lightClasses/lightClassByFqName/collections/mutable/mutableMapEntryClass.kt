// test.CMutableMapEntry
// WITH_STDLIB

package test

abstract class CMutableMapEntry<KElem, VElem> : MutableMap.MutableEntry<KElem, VElem> by mutableMapOf<KElem, VElem>().entries.first()
