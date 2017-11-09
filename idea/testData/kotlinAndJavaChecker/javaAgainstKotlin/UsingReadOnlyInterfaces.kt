class A
class B

// kotlin.collections.Collection
interface ICollection<Elem> : Collection<Elem>
abstract class CCollection<Elem> : Collection<Elem> by emptyList<Elem>()
abstract class SCollection : Collection<String> by emptyList<String>()
abstract class ACollection : Collection<A> by emptyList<A>()

// kotlin.collections.Iterable
interface IIterable<Elem> : Iterable<Elem>
abstract class CIterable<Elem> : Iterable<Elem> by emptyList<Elem>()
abstract class SIterable : Iterable<String> by emptyList<String>()
abstract class AIterable : Iterable<A> by emptyList<A>()

// kotlin.collections.List
interface IList<Elem> : List<Elem>
abstract class CList<Elem> : List<Elem> by emptyList<Elem>()
abstract class SList : List<String> by emptyList<String>()
abstract class AList : List<A> by emptyList<A>()

// kotlin.collections.Set
interface ISet<Elem> : Set<Elem>
abstract class CSet<Elem> : Set<Elem> by emptySet<Elem>()
abstract class SSet : Set<String> by emptySet<String>()
abstract class ASet : Set<A> by emptySet<A>()

// kotlin.collections.Iterator
interface IIterator<Elem> : Iterator<Elem>
abstract class CIterator<Elem> : Iterator<Elem> by emptyList<Elem>().iterator()
abstract class SIterator : Iterator<String> by emptyList<String>().iterator()
abstract class AIterator : Iterator<A> by emptyList<A>().iterator()

// kotlin.collections.Map
interface IMap<KElem, VElem> : Map<KElem, VElem>
abstract class CMap<KElem, VElem> : Map<KElem, VElem> by emptyMap<KElem, VElem>()
abstract class SMap<VElem> : Map<String, VElem> by emptyMap<String, VElem>()
abstract class ABMap : Map<A, B> by emptyMap<A, B>()

// kotlin.collections.Map.Entry
interface IMapEntry<KElem, VElem> : Map.Entry<KElem, VElem>
abstract class CMapEntry<KElem, VElem> : Map.Entry<KElem, VElem> by emptyMap<KElem, VElem>().entries.first()
abstract class SMapEntry<VElem> : Map.Entry<String, VElem> by emptyMap<String, VElem>().entries.first()
abstract class ABMapEntry : Map.Entry<A, B> by emptyMap<A, B>().entries.first()
