// test.CIterable
// WITH_STDLIB

package test

abstract class CIterable<Elem> : Iterable<Elem> by emptyList<Elem>()
