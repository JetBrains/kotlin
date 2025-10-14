// test.SMutableIterator
// WITH_STDLIB

package test

abstract class SMutableIterator : MutableIterator<String> by mutableListOf<String>().iterator()
