import java.util.Enumeration

inline fun <T> java.util.Enumeration<T>.iterator() = object: Iterator<T> {
  override fun hasNext(): Boolean = hasMoreElements()

  override fun next() = nextElement()
}

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

fun main(args : Array<String>) {
  val i : Int? = 1
  for (x in i) {
    System.out.println(x)
  }
}

