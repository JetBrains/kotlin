namespace jet.collections.iterator

import java.util.NoSuchElementException

trait IIterator<out T> {
  fun next() : T
  val hasNext : Boolean

  inline fun <R> foldLeft(init: R, operation: fun(accumulated: R, element: T) : R) : R {
    while(hasNext)
        init = operation(init, next())
    return init
  }

  inline fun <R> foldLeft(init: R, operation: fun(accumulated: R, index: Int, element: T) : R) : R {
    var k = 0
    while(hasNext)
        init = operation(init, k++, next())
    return init
  }

  inline fun foreach(operation: fun(element: T) : Unit) = while(hasNext) operation(next())

  inline fun foreach(operation: fun(index: Int, element: T) : Unit) : Unit {
    var k = 0
    while(hasNext)
        operation(k++, next())
  }

  inline fun <R> map(transform: fun(element: T) : R) : IIterator<R> {
    return object : IIterator<R> {
      override fun next() : R = transform(this@map.next())

      override val hasNext : Boolean
        get() = this@map.hasNext
    }
  }

  inline fun plus(other: IIterator<T>) : IIterator<T> =
        object : IIterator<T> {
          override fun next() : T = if(this@plus.hasNext) this@plus.next() else other.next()

          override val hasNext : Boolean
            get() = this@plus.hasNext || other.hasNext
        }

  inline fun filter(condition: fun(element: T) : Boolean) : IIterator<T> {
    return object : IIterator<T> {
      private var _next : T = null
      private var _lookedAhead : Boolean = false
      private var _hasNext : Boolean = false

      private fun lookAhead() : Boolean {
        if(!_lookedAhead) {
           _lookedAhead = true
           while(this@filter.hasNext) {
              _next = this@filter.next()
              if(condition(_next)) {
                  _hasNext = true
                  return true
              }
           }
        }
        return _hasNext
      }

      override fun next() : T {
        lookAhead()
        _lookedAhead = false
        if(_hasNext) {
            val res = _next
            _next = null
            _hasNext = false
            return res
        }
        throw NoSuchElementException()
      }

      override val hasNext : Boolean
        get() = lookAhead()
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
