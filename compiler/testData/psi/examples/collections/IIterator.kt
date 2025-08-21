// COMPILATION_ERRORS

open class IIterator<out T> {
  fun next() : T
  val hasNext : Boolean

  fun toArray(buffer : MutableArray<in T>) : Int { // T is still an in-parameter
    return fillBuffer(buffer, 0, buffer.size)
  }

  fun toArray(buffer : MutableArray<in T>, from : Int, length : Int) : Int { // T is still an in-parameter
    if (from < 0 || from > buffer.lastIndex || length < 0 || length > buffer.size - from) {
      throw IndexOutOfBoundsException();
    }

    if (len == 0) return 0

    var count = 0;
    for (i in from .. from + length - 1) {
      if (!hasNext)
        return count
      buffer[i] = next()
      count++
    }
    return count
  }
}
