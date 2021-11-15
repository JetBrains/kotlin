// WITH_STDLIB

/**
 * _Example_
 * The following will print `1`, `2` and `3` when executed:
 * ```
 * arrayOf(1, 2, 3, 4, 5)
 *   .iterator()
 *   .stopAfter { it == 3 }
 *   .forEach(::println)
 * ```
 * @return an iterator, which stops [this] Iterator after first element for which [predicate] returns `true`
 */
inline fun <T> Iterator<T>.stopAfter(crossinline predicate: (T) -> Boolean): Iterator<T> = iterator {
  for (element in this@stopAfter) {
    yield(element)
    if (predicate(element)) {
      break
    }
  }
}

fun box() =
  listOf("OK", "fail").iterator().stopAfter { it == "OK" }.next()
