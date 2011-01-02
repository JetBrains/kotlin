enum class List<out T>(theSize : Int) : IList<T> {
  Nil : List<Nothing>(0)

  Cons<T>(val value : T, val tail : List<T>) : List<T>(1 + tail.size)

  override val size : Int
    get() = theSize


  override val isEmpty : Boolean
    get() = this == Nil


  override fun iterator() = new IIterator() {
    private var current = List.this

    override val hasNext {
      get() = current == Nil
    }

    override fun next() {
      val result = current.value
      current = current.tail
      return result
    }
  }
}