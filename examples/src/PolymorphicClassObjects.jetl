open class Builder<E, R> {
  [operator] fun plusAssign(item : E)
  fun result() : R
}

open class Buildable {
  fun newBuilder<E, R>() : Builder<E, R>
}

class List<T> {

  class object Buildable {
    override fun newBuilder<E, R>() : Builder<E, R>

  }

}

extension Map<E, T>
  where
    T : Iterable<E>,
    class object T : Buildable<E, T>  for T {

  fun map<R>(f : {(E) : R}) : T<R> = {
    val builder = T.newBuilder()
    for (e in this) {
      builder += f(e)
    }
    builder.result()
  }
}