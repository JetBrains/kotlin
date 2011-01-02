class ArrayList<T> : IMutableList<T> {
  private var data = MutableArray(10)
  private var used = 0
  private var version = 0

  override fun iterator() : IIterator<T> = mutableIterator()

  override fun mutableIterator() : IMutableIterator<T> = object IMutableIterator() { // T is inferred
    private val index = 0
    private var myVersion = version

    private fun checkVersion() {
      if (version != myVersion)
        throw new ConcurrentModificationException()
    }

    override fun next() {
      checkVersion()
      if (hasNext)
        throw new NoMoreElementsException()
      data[index++]
    }

    override val hasNext
      get() = index < used


    override fun remove() {
      checkVersion()
      val result = ArrayList.this.remove(index - 1)
      myVersion = version
      result
    }
  }

  override fun get(index : Int) {
    checkIndex(index)
    data[index]
  }

  private fun checkIndex(index : Int) {
    if (index > used)
      throw new IndexOutOfBoundsException(index)
  }

  override val isEmpty
    get() = used == 0


  override val size
    get() = used


  override fun set(index : Int, value : T) {
    checkIndex(index)
    var result = data[index]
    data[index] = value
    result
  }

  override fun add(index : Int, value : T) {
    ensureSize(used + 1)
    if (index == used) {
      data[used++] = value
    } else if (index < used) {
      for (i in used-1..index) // backwards, special operator...  Need to optimize this to be a real indexed loop
        data[i + 1] = data[i]
      data[index] = value
      used++
    } else throw IndexOutOfBoundsException(index)
  }

  override fun remove(index : Int) {
    for (i in index..used-1)
     data[i] = data[i + 1]
  }
}