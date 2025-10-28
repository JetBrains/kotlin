// WITH_STDLIB
package test

abstract class CMutableSet<Elem> : MutableSet<Elem>

abstract class CMutableSet2<Elem> : MutableSet<Elem> by mutableSetOf<Elem>()
