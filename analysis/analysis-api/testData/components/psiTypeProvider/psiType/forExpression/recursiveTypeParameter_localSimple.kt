// WITH_STDLIB

fun testNotTransitive() {
  class Test(val v: Int): Comparable<Test> {
    override fun compareTo(other: Test): Int {
      return v.compareTo(-other.v)
    }
  }
  val list = listOf(Test(1), Test(2), Test(3), Test(4))
  checkTransitiveComparator(<expr>list</expr>)
}

fun <T : Comparable<T>> checkTransitiveComparator(list: List<T>) {
}
