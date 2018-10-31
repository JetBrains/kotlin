import java.util.*

/** should load cls */
class MyList : List<String> {
  override operator fun get(index: Int): String {}
}

/** should load cls */
interface ASet<T> : MutableCollection<T> {}

/** should load cls */
abstract class MySet<T> : ASet<T> {
  override fun remove(elem: String): Boolean {}

}

/** should load cls */
abstract class SmartSet<T> private constructor() : AbstractSet<T>() {
  override fun iterator(): MutableIterator<T> = unresolved

  override fun add(element: T): Boolean {
    return true
  }

  override fun contains(element: T): Boolean = true

}
