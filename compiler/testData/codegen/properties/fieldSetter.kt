var collector: String = ""
  set(it) { field = field + it }

fun append(s: String): String {
  collector = s;
  return collector;
}
