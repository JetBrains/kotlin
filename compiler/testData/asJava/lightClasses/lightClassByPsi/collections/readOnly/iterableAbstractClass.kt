// WITH_STDLIB
package test

abstract class CIterable<Elem> : Iterable<Elem>

abstract class CIterable2<Elem> : Iterable<Elem> by emptyList()
