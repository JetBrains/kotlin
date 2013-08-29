fun main() {
  for (i <caret>in "") {}
}

// MULTIRESOLVE
// REF: (for jet.CharSequence in kotlin).iterator()
// REF: (in jet.CharIterator).next()
// REF: (in jet.Iterator).hasNext()
