class A
class B

// kotlin.collection.MutableCollection
interface IMutableCollection<Elem> : MutableCollection<Elem>
abstract class CMutableCollection<Elem> : MutableCollection<Elem> by mutableListOf<Elem>()
abstract class SMutableCollection : MutableCollection<String> by mutableListOf<String>()
abstract class AMutableCollection : MutableCollection<A> by mutableListOf<A>()

// kotlin.collections.MutableIterable
interface IMutableIterable<Elem> : MutableIterable<Elem>
abstract class CMutableIterable<Elem> : MutableIterable<Elem> by mutableListOf<Elem>()
abstract class SMutableIterable : MutableIterable<String> by mutableListOf<String>()
abstract class AMutableIterable : MutableIterable<A> by mutableListOf<A>()

// kotlin.collections.MutableList
interface IMutableList<Elem> : MutableList<Elem>
abstract class CMutableList<Elem> : MutableList<Elem> by mutableListOf<Elem>()
abstract class SMutableList : MutableList<String> by mutableListOf<String>()
abstract class AMutableList : MutableList<A> by mutableListOf<A>()

// kotlin.collections.Set
interface IMutableSet<Elem> : MutableSet<Elem>
abstract class CMutableSet<Elem> : MutableSet<Elem> by mutableSetOf<Elem>()
abstract class SMutableSet : MutableSet<String> by mutableSetOf<String>()
abstract class AMutableSet : MutableSet<A> by mutableSetOf<A>()

// kotlin.collections.Iterator
interface IMutableIterator<Elem> : MutableIterator<Elem>
abstract class CMutableIterator<Elem> : MutableIterator<Elem> by mutableListOf<Elem>().iterator()
abstract class SMutableIterator : MutableIterator<String> by mutableListOf<String>().iterator()
abstract class AMutableIterator : MutableIterator<A> by mutableListOf<A>().iterator()

// kotlin.collections.Map
interface IMutableMap<KElem, VElem> : MutableMap<KElem, VElem>
abstract class CMutableMap<KElem, VElem> : MutableMap<KElem, VElem> by mutableMapOf<KElem, VElem>()
abstract class SMutableMap<VElem> : MutableMap<String, VElem> by mutableMapOf<String, VElem>()
abstract class ABMutableMap : MutableMap<A, B> by mutableMapOf<A, B>()

// kotlin.collections.Map.Entry
interface IMutableMapEntry<KElem, VElem> : MutableMap.MutableEntry<KElem, VElem>
abstract class CMutableMapEntry<KElem, VElem> : MutableMap.MutableEntry<KElem, VElem> by mutableMapOf<KElem, VElem>().entries.first()
abstract class SMutableMapEntry<VElem> : MutableMap.MutableEntry<String, VElem> by mutableMapOf<String, VElem>().entries.first()
abstract class ABMutableMapEntry : MutableMap.MutableEntry<A, B> by mutableMapOf<A, B>().entries.first()
