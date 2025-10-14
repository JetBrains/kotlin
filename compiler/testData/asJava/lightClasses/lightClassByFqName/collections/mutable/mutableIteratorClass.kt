// test.CMutableIterator
// WITH_STDLIB

package test

abstract class CMutableIterator<Elem> : MutableIterator<Elem> by mutableListOf<Elem>().iterator()
