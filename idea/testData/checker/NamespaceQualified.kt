package a

    import java.*
    import java.util.*

/////////////////////////////////////////////////////////////////////////

fun done<O>(result : O) : Iteratee<Any?, O> = StrangeIterateeImpl<Any?, O>(result)

abstract class Iteratee<in I, out O> {
  abstract fun process(item : I) : Iteratee<I, O>
  abstract val isDone : Boolean
  abstract val result : O
  abstract fun done() : O
}

class StrangeIterateeImpl<in I, out O>(val obj: O) : Iteratee<I, O>() {
    override fun process(item: I): Iteratee<I, O> = StrangeIterateeImpl<I, O>(obj)
    override val isDone = true
    override val result = obj
    override fun done() = obj
}

abstract class Sum() : Iteratee<Int, Int>() {
  override fun process(item : Int) : Iteratee<Int, Int> {
    return a.done<Int>(item);
  }
  abstract override val isDone : Boolean
  abstract override val result : Int
  abstract override fun done() : Int
}

abstract class Collection<E> : Iterable<E> {
  fun iterate<O>(iteratee : Iteratee<E, O>) : O {
      var current = iteratee
      for (x in this) {
        val it = current.process(x)
        if (it.isDone) return it.result
        current = it
      }
      return current.done()
  }
}