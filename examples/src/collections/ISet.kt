namespace jet.collections.set

import jet.collections.sized.ISized
import jet.collections.iterable.IIterable

trait ISet<T> : IIterable<T>, ISized {
  fun contains(item : T) : Boolean
}
