fun main(it: Iterator<Any>) {
  for (i <caret>in it.iterator()) {}
}

// MULTIRESOLVE
// REF: (for kotlin.Iterator<T> in kotlin).iterator()
// REF: (in kotlin.Iterator).hasNext()
// REF: (in kotlin.Iterator).next()
