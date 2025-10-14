// test.SMutableSet
// WITH_STDLIB

package test

abstract class SMutableSet : MutableSet<String> by mutableSetOf<String>()
