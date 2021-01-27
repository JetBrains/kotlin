class Coll {
  operator fun iterator(): It? = null
}

class It {
  operator fun next() = 1
  operator fun hasNext() = false
}

fun test() {
  <!UNSAFE_CALL, UNSAFE_CALL!>for (x in Coll()) {}<!>
}