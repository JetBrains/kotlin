// IGNORE_FIR
fun main() {
  for (i <caret>in 1..2) {}
}

// MULTIRESOLVE
// REF: (in kotlin.collections.IntIterator).next()
// REF: (in kotlin.ranges.IntProgression).iterator()
// REF: (in kotlin.collections.Iterator).hasNext()
