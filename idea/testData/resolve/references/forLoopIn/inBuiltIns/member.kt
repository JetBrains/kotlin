fun main() {
  for (i <caret>in 1..2) {}
}

// MULTIRESOLVE
// REF: (in jet.IntIterator).next()
// REF: (in jet.IntRange).iterator()
// REF: (in jet.Iterator).hasNext()
