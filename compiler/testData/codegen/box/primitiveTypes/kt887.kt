// IGNORE_BACKEND_FIR: JVM_IR
class Book(val name: String) : Comparable<Book> {
  override fun compareTo(other: Book) = name.compareTo(other.name)
}

fun box() = if(Book("239").compareTo(Book("932")) != 0) "OK" else "fail"
