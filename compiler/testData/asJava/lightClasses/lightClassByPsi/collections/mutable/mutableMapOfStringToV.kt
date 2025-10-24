// test.SMutableMap
// WITH_STDLIB

package test

abstract class SMutableMap<VElem> : MutableMap<String, VElem> by mutableMapOf<String, VElem>()
