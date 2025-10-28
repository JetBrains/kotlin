// WITH_STDLIB
package test

abstract class SMutableMap<VElem> : MutableMap<String, VElem>

abstract class SMutableMap2<VElem> : MutableMap<String, VElem> by mutableMapOf<String, VElem>()
