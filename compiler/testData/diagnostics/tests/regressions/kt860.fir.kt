// !DIAGNOSTICS: -NOTHING_TO_INLINE
// KT-860 ConcurrentModificationException in frontend

package std.util

import java.util.*

fun <T, U: MutableCollection<in T>> Iterator<T>.to(container: U) : U {
    while(hasNext())
        container.add(next())
    return container
}

fun <T> Iterator<T>.toArrayList() = to(ArrayList<T>())
