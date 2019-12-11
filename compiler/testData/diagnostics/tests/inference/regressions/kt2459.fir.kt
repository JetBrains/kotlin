//KT-2459 Type inference error
package b

import java.util.*

class B<T>(val x: List<T>)
fun <T> f(x: T): B<T> = B(arrayList(x))

// from standard library
fun <T> arrayList(vararg values: T) : ArrayList<T> {}