package a

//+JDK
import java.util.*

fun foo() {
    val v = array(1, 2, 3)

    val u = v map { it * 2 }

    u : List<Int>
}


// ---------------------
// copy from kotlin util

fun <T> array(vararg t : T) : Array<T> = t

fun <T, R> Array<T>.map(transform : (T) -> R) : java.util.List<R> {
    return mapTo(java.util.ArrayList<R>(this.size), transform)
}

fun <T, R, C: Collection<in R>> Array<T>.mapTo(result: C, transform : (T) -> R) : C {
  for (item in this)
    result.add(transform(item))
  return result
}
