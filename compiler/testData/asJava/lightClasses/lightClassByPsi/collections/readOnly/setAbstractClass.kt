// WITH_STDLIB
package test

abstract class CSet<Elem> : Set<Elem>

abstract class CSet2<Elem> : Set<Elem> by emptySet<Elem>()
