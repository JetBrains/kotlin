// WITH_STDLIB
package test

abstract class SMutableIterator : MutableIterator<String>

abstract class SMutableIterator2 : MutableIterator<String> by mutableListOf<String>().iterator()
