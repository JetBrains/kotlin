// WITH_STDLIB
package test

abstract class SList : List<String>

abstract class SList2 : List<String> by emptyList<String>()
