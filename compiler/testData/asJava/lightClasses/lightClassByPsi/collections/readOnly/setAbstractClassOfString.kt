// WITH_STDLIB
package test

abstract class SSet : Set<String>

abstract class SSet2 : Set<String> by emptySet<String>()
