// WITH_STDLIB
package test

abstract class CIterator<Elem> : Iterator<Elem>

abstract class CIterator2<Elem> : Iterator<Elem> by emptyList<Elem>().iterator()
