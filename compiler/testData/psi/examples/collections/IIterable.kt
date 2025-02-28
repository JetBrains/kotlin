open class IIterable<out T> {
  fun iterator() : IIterator<T>
}

// COMPILATION_ERRORS
