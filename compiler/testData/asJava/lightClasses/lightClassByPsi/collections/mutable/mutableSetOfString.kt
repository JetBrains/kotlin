// WITH_STDLIB
package test

abstract class SMutableSet : MutableSet<String>

abstract class SMutableSet2 : MutableSet<String> by mutableSetOf<String>()
