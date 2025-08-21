// COMPILATION_ERRORS

open class IMutableList<T> : IList<T>, IMutableIterable<T> {
  fun set(index : Int, value : T) : T
  fun add(index : Int, value : T)
  fun remove(index : Int) : T
  fun mutableIterator() : IMutableIterator<T>
}