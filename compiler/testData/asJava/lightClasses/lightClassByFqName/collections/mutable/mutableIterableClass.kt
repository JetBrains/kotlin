// test.CMutableIterable
// WITH_STDLIB

package test

abstract class CMutableIterable<Elem> : MutableIterable<Elem> by mutableListOf<Elem>()
