// COMPILATION_ERRORS

open class IMutableSet<T> : ISet<T>, IMutableIterable<T> {
  fun add(item : T) : Boolean
  fun remove(item : T) : Boolean
}