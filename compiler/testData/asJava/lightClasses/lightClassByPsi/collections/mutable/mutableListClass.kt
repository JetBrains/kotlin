// WITH_STDLIB
package test

abstract class CMutableList<Elem> : MutableList<Elem>

abstract class CMutableList2<Elem> : MutableList<Elem> by mutableListOf<Elem>()
