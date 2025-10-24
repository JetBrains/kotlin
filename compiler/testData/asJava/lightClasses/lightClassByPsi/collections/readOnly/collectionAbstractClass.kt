// WITH_STDLIB
package test

abstract class CCollection<Elem> : Collection<Elem>

abstract class CCollection2<Elem> : Collection<Elem> by emptyList()
