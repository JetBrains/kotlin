// WITH_STDLIB
package test

abstract class SMutableList : MutableList<String>

abstract class SMutableList2 : MutableList<String> by mutableListOf<String>()
