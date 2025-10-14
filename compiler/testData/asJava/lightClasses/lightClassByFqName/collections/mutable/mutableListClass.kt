// test.CMutableList
// WITH_STDLIB

package test

abstract class CMutableList<Elem> : MutableList<Elem> by mutableListOf<Elem>()
