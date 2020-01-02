// KT-336 Can't infer type parameter for ArrayList in a generic function (Exception in type inference)
// KT-335 Type inference fails on Collections.sort

import java.util.*

fun <T : Comparable<T>> MutableList<T>.sort() {
  Collections.sort(this) // Error here
}

fun <T> List<T>.plus(other : List<T>) : List<T> {
  val result = ArrayList(this)
  return result
}
