class Coll {
  operator fun iterator(): It = It()
}

class It {
  operator fun next() = 1
  operator fun hasNext() = false
}

fun test(c: Coll?) {
  <!INAPPLICABLE_CANDIDATE!>for (x in c) {}<!>

  if (c != null) {
    for(x in c) {}
  }
}
