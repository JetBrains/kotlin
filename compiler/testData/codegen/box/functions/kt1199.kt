
trait MyIterator<T> {
    fun hasNext() : Boolean
    fun next() : T
}

fun <T : Any> T?.iterator() = object : MyIterator<T> {
    var hasNext = this@iterator != null
      private set
    override fun hasNext() = hasNext

    override fun next() : T {
        if (hasNext) {
            hasNext = false
            return this@iterator!!
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
