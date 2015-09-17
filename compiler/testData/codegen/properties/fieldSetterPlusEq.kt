var collector: String = ""
  set(it) { field += it }

fun append(s: String): String {
  collector = s;
  return collector;
}
