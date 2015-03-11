open class Builder<E, R> {
  [operator] fun plusAssign(item : E)
  fun result() : R
}

open class Buildable {
  fun newBuilder<E, R>() : Builder<E, R>
}

class List<T> {

  default object : Buildable {
    override fun newBuilder<E, R>() : Builder<E, R>

  }

}

fun <E, T, R> Map<E, T>.map(f :  (E) -> R) : T<R> where
  T : Iterable<E>,
  class object T : Buildable<E, T>  = {
  val builder = T.newBuilder()
  for (e in this) {
    builder += f(e)
  }
  builder.result()
}