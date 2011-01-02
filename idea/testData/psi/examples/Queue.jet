class Queue<T> : IPushPop<T> {
  private class Item<T>(val data : T, var next : Item<T>)

  private var head : Item<T> = null
  private var tail : Item<T> = null

  override fun push(item : T) {
    val i = new Item(item)
    if (tail == null) {
      head = i
      tail = head
    } else {
      tail.next = i
      tail = i
    }
  }

  override fun pop() =
    if (head == null)
      throw new UnderflowException()
    else {
      val result = head.data
      head = head.next
      if (head == null)
        tail = null
      result
    }

  override val isEmpty
    get() = head == null


}