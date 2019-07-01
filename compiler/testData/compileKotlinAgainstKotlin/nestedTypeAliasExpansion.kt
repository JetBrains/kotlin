// FILE: lib.kt

package lib

class Inv<K>

typealias A<V> = Inv<V>
typealias B<T> = Inv<A<T>>

fun <U> materialize(): B<U>? = null

// FILE: main.kt

import lib.*

fun box(): String {
    val s = { materialize<Unit>() }
    return "OK"
}