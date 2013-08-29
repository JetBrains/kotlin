fun main(it: Iterator<Any>) {
  for (i <caret>in it.iterator()) {}
}

// MULTIRESOLVE
// REF: (for Iterator<T> in jet).iterator()
// REF: (in jet.Iterator).hasNext()
// REF: (in jet.Iterator).next()
