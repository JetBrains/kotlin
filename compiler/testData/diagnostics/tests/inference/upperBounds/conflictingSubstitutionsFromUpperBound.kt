// FIR_IDENTICAL
// CHECK_TYPE

package g

import java.util.HashSet
import checkType
import _

fun <T, C: Collection<T>> convert(src: Collection<T>, dest: C): C = throw Exception("$src $dest")

fun test(l: List<Int>) {
    //todo should be inferred
    val r = convert(l, HashSet())
    r checkType { _<HashSet<Int>>() }
}
