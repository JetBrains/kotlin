// FIR_IDENTICAL
fun foo(l: List<String>?) {
  Pair(l?.joinToString(), "")
}
