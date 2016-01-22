fun main() {
  for (i <caret>in "") {}
}

// MULTIRESOLVE
// REF: (for kotlin.CharSequence in kotlin.text).iterator()
// REF: (in kotlin.collections.CharIterator).next()
// REF: (in kotlin.collections.Iterator).hasNext()
