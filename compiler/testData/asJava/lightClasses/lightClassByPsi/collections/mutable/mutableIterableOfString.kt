// WITH_STDLIB
package test

abstract class SMutableIterable : MutableIterable<String>

abstract class SMutableIterable2 : MutableIterable<String> by mutableListOf<String>()
