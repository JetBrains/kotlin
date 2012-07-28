// FILE: b.kt


package foobar.a
    import java.*

    val a : util.List<Int>? = null
    val a1 : <!UNRESOLVED_REFERENCE!>List<!><Int>? = null

// FILE: b.kt
package foobar

abstract class Foo<T>() {
    abstract val x : T<Int>
}

// FILE: b.kt
package foobar.a
    import java.util.*

    val b : List<Int>? = a
    val b1 : <!UNRESOLVED_REFERENCE!>util<!>.List<Int>? = a

// FILE: b.kt
package foobar
val x1 = a.a

val y1 = a.b


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
    return foobar.done<Int>(item);
  }
  abstract override val isDone : Boolean
  abstract override val result : Int
  abstract override fun done() : Int
}

abstract class Collection<E> : Iterable<E> {
  fun iterate<O>(var iteratee : Iteratee<E, O>) : O {
      for (x in this) {
        val it = iteratee.process(x)
        if (it.isDone) return it.result
        iteratee = it
      }
      return iteratee.done()
  }
}
