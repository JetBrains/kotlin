open class IMutableIterable<out T> : IIterable<T> {
  fun mutableIterator() : IMutableIterator<T>
}
// COMPILATION_ERRORS
