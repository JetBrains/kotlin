// COMPILATION_ERRORS

class Stack<T> : IPushPop<T> {
  private val data = ArrayList<T>();

  override fun push(item : T) {
    data.add(item) // Problem: I would like to write push(...) = data.add(...), but the types do not match
  }

  override fun pop() = data.removeLast()

  override val isEmpty
    get() = data.isEmpty

}