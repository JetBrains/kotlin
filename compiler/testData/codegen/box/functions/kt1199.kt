interface MyIterator<T> {
    operator fun hasNext() : Boolean
    operator fun next() : T
}

operator fun <T : Any> T?.iterator() = object : MyIterator<T> {
    private var hasNext = this@iterator != null

    override fun hasNext() = hasNext

    override fun next() : T {
        if (hasNext) {
            hasNext = false
            return this@iterator!!
        }
        throw NoSuchElementException()
    }
}

fun box() : String {
  var k = 0
  for (i in 1) {
    k++
  }
  return if(k == 1) "OK" else "fail"
}
