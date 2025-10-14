// test.SMutableList
// WITH_STDLIB

package test

abstract class SMutableList : MutableList<String> by mutableListOf<String>()
