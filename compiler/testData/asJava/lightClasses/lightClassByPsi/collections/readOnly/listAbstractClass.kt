// WITH_STDLIB
package test

abstract class CList<Elem> : List<Elem>

abstract class CList2<Elem> : List<Elem> by emptyList<Elem>()
