// WITH_STDLIB
package test

interface ICollection<Elem> : Collection<Elem>

abstract class CCollection<Elem> : ICollection<Elem>

abstract class CCollection2<Elem> : CCollection<Elem>()