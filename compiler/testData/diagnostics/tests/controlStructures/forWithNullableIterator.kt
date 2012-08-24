class Coll {
  fun iterator(): It? = null
}

class It {
  fun next() = 1
  fun hasNext() = false
}

fun test() {
  for (x in <!HAS_NEXT_FUNCTION_NONE_APPLICABLE, NEXT_NONE_APPLICABLE!>Coll()<!>) {}
}