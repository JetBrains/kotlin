// WITH_STDLIB
package test

abstract class CMutableMap<KElem, VElem> : MutableMap<KElem, VElem>

abstract class CMutableMap2<KElem, VElem> : MutableMap<KElem, VElem> by mutableMapOf<KElem, VElem>()
