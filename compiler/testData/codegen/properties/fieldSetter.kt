var collector: String = ""
  set(it) { $collector = $collector + it }

fun append(s: String): String {
  collector = s;
  return collector;
}
