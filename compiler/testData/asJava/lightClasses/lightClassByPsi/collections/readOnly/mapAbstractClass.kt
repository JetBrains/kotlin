// WITH_STDLIB
package test

abstract class CMap<KElem, VElem> : Map<KElem, VElem>

abstract class CMap2<KElem, VElem> : Map<KElem, VElem> by emptyMap<KElem, VElem>()