class Coll {
  fun iterator(): It = It()
}

class It {
  fun next() = 1
  fun hasNext() = false
}

fun test(c: Coll?) {
  for (x in <!ITERATOR_MISSING!>c<!>) {}

  if (c != null) {
    for(x in <!DEBUG_INFO_SMARTCAST!>c<!>) {}
  }
}