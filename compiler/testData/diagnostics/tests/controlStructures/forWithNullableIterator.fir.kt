class Coll {
  operator fun iterator(): It? = null
}

class It {
  operator fun next() = 1
  operator fun hasNext() = false
}

fun test() {
  <!INAPPLICABLE_CANDIDATE, INAPPLICABLE_CANDIDATE!>for (x in Coll()) {}<!>
}