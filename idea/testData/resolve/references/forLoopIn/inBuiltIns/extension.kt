// IGNORE_FIR

fun main(it: Iterator<Any>) {
  for (i <caret>in it.iterator()) {}
}

// MULTIRESOLVE
// REF: (for kotlin.collections.Iterator<T> in kotlin.collections).iterator()
// REF: (in kotlin.collections.Iterator).hasNext()
// REF: (in kotlin.collections.Iterator).next()
