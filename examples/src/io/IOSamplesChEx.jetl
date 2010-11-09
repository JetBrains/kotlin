interface class IIterable<T> throws <E> {
  fun next() : T // implicitly throws  <E>
  val hasNext : Boolean // implicitly throws <E>, but actually may NOT!!!
}

interface class IAdder<T> throws <E> {
  fun add(item : T) : Boolean // implicitly throws <E>
}

interface class ICloseable throws <E> {
  fun close()
}

abstract class JavaCloseableWrapper(closeable : java.io.Closeable) throws <IOException> : ICloseable { // Maybe we can delegate here
  override fun close() {
    closeable.close()
  }
}

fun streamCopy<T>(from : IIterable<T>, to : IAdder<T>) { // implicitly rethrows
  for (item in from) t.add(item) // both implicitly throw <E>
}

class FileInput throws <IOException> : IIterable<Byte>, JavaCloseableWrapper {
  private val stream : InputStream
  private var next : Int
  private var nextUsed = false

  this(file : File) : JavaCloseableWrapper(stream) { // implicitly throws IOException
    stream = FileInputStream(file) // throws IOException
  }

  override fun next() {
    if (!nextUsed) {
      nextUsed = true
      return next.as<Byte>
    }
    return
  }
  
  override val hasNext {
    get() { // implicitly throws IOException
      if (nextUsed && next != -1) {
        nextUsed = false
        next = stream.read() // throws IOException
      }
      return next != -1        
    }
  }
}

class FileOutput throws IOException : IAdder<Byte>, JavaCloseableWrapper {
  private val stream : OutputStream

  this(file : File) : JavaCloseableWrapper(stream) {
    stream = FileOutputStream(file)
  }

  override fun add(item : Byte) {
    stream.write(item)
  }
}

fun example() { // this does not rethrow, no appropriate parameters given

  val f1 : File = ...
  val f2 : File = ...

  streamCopy(FileInput(f1), f2) // throws IOException, you must catch or rethrow explicitly

}