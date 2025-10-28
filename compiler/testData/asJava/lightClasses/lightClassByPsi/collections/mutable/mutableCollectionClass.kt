// WITH_STDLIB
package test

abstract class CMutableCollection<Elem> : MutableCollection<Elem>

abstract class CMutableCollection2<Elem> : MutableCollection<Elem> by mutableListOf<Elem>()
