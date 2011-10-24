namespace jet.collections

import java.util.NoSuchElementException

trait Iterator<out T> {
  fun next()  : T
  val hasNext : Boolean

  fun <R> map(transform: fun(element: T) : R) : Iterator<R> =
        object : Iterator<R> {
            override fun next() : R = transform(this@map.next())

            override val hasNext : Boolean
                get() = this@map.hasNext
        }
}

trait ISized {
  val size : Int
}

val ISized.isEmpty : Boolean
    get() = size == 0

val ISized.isNonEmpty : Boolean
    get() = size != 0

trait ISet<T> : Iterable<T>, ISized {
  fun contains(item : T) : Boolean
}

trait IList<out T> : Iterable<T>, ISized {
  fun get(index : Int) : T

  fun iterator() : Iterator<T> =
    object Iterator<T> {
        var index = 0

        override fun next() : T = get(index++)

        override val hasNext : Boolean
           get() = index < size
    }

  fun toArray() : Array<T> = Array<T>(size) { index => get(index) }
}

fun <T> Array<T>.asList() : IList<T> = object IList<T> {
    override val size
        get() = this@Array.size

    override fun get(index: Int) = this@asList[index]
}

fun <T> Iterator<T>.foreach(operation: fun(element: T) : Unit)  : Unit = while(hasNext) operation(next())

fun <T> Iterator<T>.foreach(operation: fun(index: Int, element: T) : Unit) : Unit {
    var k = 0
    while(hasNext)
        operation(k++, next())
}

fun <T> Iterable<T>.foreach(operation: fun(element: T) : Unit) : Unit = iterator() foreach operation

fun <T> Iterable<T>.foreach(operation: fun(index: Int, element: T) : Unit) : Unit = iterator() foreach operation

fun <T,R> Iterable<T>.foldLeft(init: R, operation: fun(accumulated: R, element: T) : R) : R =
    iterator () foreach operation

fun <T,R> Iterable<T>.foldLeft(init: R, operation: fun(accumulated: R, index: Int, element: T) : R) : R =
    iterator () foreach operation

fun <T,R> Iterator<T>.foldLeft(init: R, operation: fun(accumulated: R, element: T) : R) : R {
    while(hasNext)
        init = operation(init, next())
    return init
}

fun <T,R> Iterator<T>.foldLeft(init: R, operation: fun(accumulated: R, index: Int, element: T) : R) : R {
    var k = 0
    while(hasNext)
        init = operation(init, k++, next())
    return init
}

fun <T,R> Iterator<T>.map(transform: fun(element: T) : R) : Iterator<R> =
    object : Iterator<R> {
      override fun next() : R = transform(this@map.next())

      override val hasNext : Boolean
        get() = this@map.hasNext
    }

fun <T,R> Iterator<T>.map(transform: fun(index: Int, element: T) : R) : Iterator<R> {
    return object : Iterator<R> {
      var index = 0

      override fun next() : R = transform(index++, this@map.next())

      override val hasNext : Boolean
        get() = this@map.hasNext
    }
}

fun <T> Iterator<T>.plus(other: Iterator<T>) : Iterator<T> =
    object : Iterator<T> {
      override fun next() : T = if(this@plus.hasNext) this@plus.next() else other.next()

      override val hasNext : Boolean
        get() = this@plus.hasNext || other.hasNext
    }

fun <T> Iterator<T>.filter(condition: fun(element: T) : Boolean) : Iterator<T> {
    return object : Iterator<T> {
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
