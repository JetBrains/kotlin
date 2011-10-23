trait IList<out T> : IIterable<T>, ISized {
  fun get(index : Int) : T
}