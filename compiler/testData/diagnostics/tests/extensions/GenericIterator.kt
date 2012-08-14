import java.util.Enumeration

inline fun <T> java.util.Enumeration<T>.iterator() = object: Iterator<T> {
  override fun hasNext(): Boolean = hasMoreElements()

  override fun next() = nextElement()
}

fun <T : Any> T?.iterator() = object {
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

fun main(args : Array<String>) {
  val i : Int? = 1
  for (x in i) {
    System.out.println(x)
  }
}
