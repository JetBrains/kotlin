// WITH_STDLIB
package test

abstract class SIterable : Iterable<String>

abstract class SIterable2 : Iterable<String> by emptyList<String>()
