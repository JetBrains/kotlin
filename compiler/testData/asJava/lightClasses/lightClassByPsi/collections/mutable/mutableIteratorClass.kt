// WITH_STDLIB
package test

abstract class CMutableIterator<Elem> : MutableIterator<Elem>

abstract class CMutableIterator2<Elem> : MutableIterator<Elem> by mutableListOf<Elem>().iterator()
