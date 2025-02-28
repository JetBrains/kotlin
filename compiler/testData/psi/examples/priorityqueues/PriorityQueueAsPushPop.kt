// COMPILATION_ERRORS

class PriorityQueueAsPushPop<T>(wrapped : IPriorityQueue<T>) : IPushPop<T> {
  override fun pop() = wrapped.extract()
  override fun push(item : T) = wrapped.add(item)
  override val isEmpty
    get() = wrapped.isEmpty

}