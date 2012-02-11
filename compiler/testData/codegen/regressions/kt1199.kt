fun <erased T : Any> T?.iterator() = object {
    var hasNext = this@iterator != null
      private set

    fun next() : T {
        if (hasNext) {
            hasNext = false
            return this@iterator.sure()
        }
        throw java.util.NoSuchElementException()
    }
}

fun box() : String {
  var k = 0
  for (i in 1) {
    k++
  }
  return if(k == 1) "OK" else "fail"
}
