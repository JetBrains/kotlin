// COMPILATION_ERRORS

open class IMutableIterator<out T> : IIterator<T> {
  fun remove() : T

/*
  Considerations:
    pro: why not + non iteration breaking
    con: counter-intuitive for, e.g., TreeSet


  fun addBefore(item : T) : Boolean
  fun addAfter(item : T) : Boolean
*/
}