// !WITH_NEW_INFERENCE
package a

import java.util.*

fun <T> g (f: () -> List<T>) : T {}

fun test() {
    //here possibly can be a cycle on constraints
    val x = g { Collections.emptyList() }

    val y = g<Int> { Collections.emptyList() }
    val z : List<Int> = g { Collections.emptyList() }
}