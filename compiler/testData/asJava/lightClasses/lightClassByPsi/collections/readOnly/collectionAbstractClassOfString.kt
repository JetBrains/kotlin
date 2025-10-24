// WITH_STDLIB
package test

abstract class SCollection : Collection<String>

abstract class SCollection2 : Collection<String> by emptyList<String>()
