class Coll {
  operator fun iterator(): It = It()
}

class It {
  operator fun next() = 1
  operator fun hasNext() = false
}

fun test(c: Coll?) {
  <!INAPPLICABLE_CANDIDATE, UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE!>for (x in c) {}<!>

  if (c != null) {
    for(x in c) {}
  }
}