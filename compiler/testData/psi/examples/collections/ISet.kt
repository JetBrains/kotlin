open class ISet<T> : IIterable<T>, ISized {
  fun contains(item : T) : Boolean
}

// COMPILATION_ERRORS
