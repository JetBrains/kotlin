//KT-2179 Nested function literal breaks compiler
package i

//+JDK
import java.util.*

fun test() {
    val sample1: List<List<Int?>> = arrayList(arrayList<Int?>(1, 7, null, 8))

    //breaks compiler
    val sample2 = sample1.map({it.map({it})})
    sample2 : List<List<Int?>>

    //breaks compiler
    val sample3 = sample1.map({row -> row.map({column -> column})})
    sample3 : List<List<Int?>>

    //doesn't break compiler
    val identity: (Int?) -> Int? = {column -> column}
    val sample4 = sample1.map({row -> row.map(identity)})
    sample4 : List<List<Int?>>
}

//------------

fun arrayList<T>(vararg values: T) : ArrayList<T> = values.toCollection(ArrayList<T>(values.size()))

fun <T, R> Collection<T>.map(transform : (T) -> R) : List<R> {
    return mapTo(java.util.ArrayList<R>(this.size), transform)
}

fun <T, R, C: MutableCollection<in R>> Collection<T>.mapTo(result: C, transform : (T) -> R) : C {
  for (item in this)
    result.add(transform(item))
  return result
}

fun <T, C: MutableCollection<in T>> Array<T>.toCollection(result: C) : C {
    for (element in this) result.add(element)
    return result
}

val Collection<*>.size : Int
    get() = size()
