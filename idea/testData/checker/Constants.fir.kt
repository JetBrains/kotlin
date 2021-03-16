fun <T> checkSubtype(t: T) = t

fun test() {
  checkSubtype<Byte>(1)
  checkSubtype<Int>(1)
  <error descr="[INAPPLICABLE_CANDIDATE] Inapplicable candidate(s): /checkSubtype">checkSubtype</error><Double>(1)
  1 as Byte
  1 as Int
  1 as Double
}
