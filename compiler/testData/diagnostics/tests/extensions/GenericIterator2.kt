import java.util.Enumeration

inline fun <T> java.util.Enumeration<T>.iterator() = object : Iterator<T> {
  public override val hasNext: Boolean
    get() = hasMoreElements()

  public override fun next() = nextElement()
}

fun a(e : java.util.Enumeration<Int>) {
    for (i in e) {
        i : Int
    }
}
