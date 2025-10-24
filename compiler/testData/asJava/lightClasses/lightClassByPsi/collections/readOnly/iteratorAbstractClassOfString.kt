// WITH_STDLIB
package test

abstract class SIterator : Iterator<String>

abstract class SIterator2 : Iterator<String> by emptyList<String>().iterator()
