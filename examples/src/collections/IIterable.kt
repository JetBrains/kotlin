package jet.collections.iterable

import jet.collections.iterator.IIterator

trait IIterable<out T> {
  fun iterator() : IIterator<T>

  inline fun foreach(operation: fun(element: T) : Unit) = iterator() foreach operation
}
