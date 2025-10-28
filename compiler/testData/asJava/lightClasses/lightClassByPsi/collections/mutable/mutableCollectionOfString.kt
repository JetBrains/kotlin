// WITH_STDLIB
package test

abstract class SMutableCollection : MutableCollection<String>

abstract class SMutableCollection2 : MutableCollection<String> by mutableListOf<String>()
