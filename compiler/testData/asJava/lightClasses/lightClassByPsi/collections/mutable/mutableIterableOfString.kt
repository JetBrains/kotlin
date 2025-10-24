// test.SMutableIterable
// WITH_STDLIB

package test

abstract class SMutableIterable : MutableIterable<String> by mutableListOf<String>()
