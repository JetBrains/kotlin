// WITH_STDLIB
package test

abstract class CMutableIterable<Elem> : MutableIterable<Elem>

abstract class CMutableIterable2<Elem> : MutableIterable<Elem> by mutableListOf<Elem>()
