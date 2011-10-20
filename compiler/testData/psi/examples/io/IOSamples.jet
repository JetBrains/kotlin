open class IAdder<in T> {
  fun add(item : T) : Boolean
}

open class ICloseable {
  fun close()
}

abstract class JavaCloseableWrapper(closeable : java.io.Closeable) : ICloseable(closeable)

fun streamCopy<T>(from : IIterable<T>, to : IAdder<T>) {
  for (item in from) t.add(item)
}

class FileInput : IIterator<Byte>, JavaCloseableWrapper {
  private val stream : InputStream
  private var next : Int
  private var nextUsed = false

  this(file : File) : JavaCloseableWrapper(stream) { // implicitly throws IOException
    stream = FileInputStream(file) // throws IOException
  }

  override fun next() {
    if (!nextUsed) {
      nextUsed = true
      return next as Byte
    }
    return
  }
  
  override val hasNext
    get() { // implicitly throws IOException
      if (nextUsed && next != -1) {
        nextUsed = false
        next = stream.read() // throws IOException
      }
      return next != -1        
    }

}

class FileOutput : IAdder<Byte>, JavaCloseableWrapper {
  private val stream : OutputStream

  this(file : File) : JavaCloseableWrapper(stream) {
    stream = FileOutputStream(file)
  }

  override fun add(item : Byte) {
    stream.write(item)
  }
}

fun example() { // this does not rethrow, no appropriate parameters given

  val f1 : File //= ...
  val f2 : File //= ...

  streamCopy(FileInput(f1), f2) // throws IOException, you must catch or rethrow explicitly

}