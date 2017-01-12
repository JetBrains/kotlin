// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS


interface MyIterator<T> {
    operator fun hasNext() : Boolean
    operator fun next() : T
}

operator fun <T : Any> T?.iterator() = object : MyIterator<T> {
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
