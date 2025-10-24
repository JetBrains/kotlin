// test.SIterator
// WITH_STDLIB

package test

abstract class SIterator : Iterator<String> by emptyList<String>().iterator()
