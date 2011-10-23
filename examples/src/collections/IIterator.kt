namespace jet.collections.iterator

trait IIterator<out T> {
  fun next() : T
  val hasNext : Boolean

  inline fun foreach(operation: fun(element: T) : Unit) = while(hasNext) operation(next())

  inline fun <R> map(transform: fun(element: T) : R) : IIterator<R> {
    val that = this
    return object : IIterator<R> {
      override fun next() : R = transform(that.next())

      override val hasNext : Boolean
        get() = that.hasNext
    }
  }
/*
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
*/
}
