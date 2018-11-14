
abstract class C<T>(var constructorParam: List<CharSequence>) {
  fun foo<V, U : V>(p1: V, p2: C<V>, p4: Sequence<V>): T {}

  inline fun <reified T : Enum<T>> printAllValues() {
    print(enumValues<T>().joinToString { it.name })
  }

  val <Q : T> Q.w: Q get() = null!!

  var sListProp: List<String>?
  var sSetProp: Set<String>?
  var sMutableSetProp: MutableSet<String>?
  var sHashSetProp: HashSet<String>?
  var csListProp: List<CharSequence>?

  abstract fun listCS(l: List<CharSequence>): List<CharSequence>
  abstract fun listS(l: List<String>): List<String>
  abstract fun mutables(cin: MutableCollection<in Number>, sOut: MutableList<out C<*>>): MutableSet<CharSequence>
  abstract fun nested(l: List<List<CharSequence>>): Collection<Collection<CharSequence>>

  fun <T : Any?> max(p0 : Collection<T>?): T?  where T : Comparable<T>? {}

}

open class K<out T: K<T>> { }
class Sub: K<K<*>>()
