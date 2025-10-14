// test.SMutableCollection
// WITH_STDLIB

package test

abstract class SMutableCollection : MutableCollection<String> by mutableListOf<String>()
