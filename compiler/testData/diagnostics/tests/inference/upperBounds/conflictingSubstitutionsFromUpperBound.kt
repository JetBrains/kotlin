// !CHECK_TYPE

package g

import java.util.HashSet
fun <T, C: Collection<T>> convert(src: Collection<T>, dest: C): C = throw Exception("$src $dest")

fun test(l: List<Int>) {
    //todo should be inferred
    val r = <!TYPE_INFERENCE_UPPER_BOUND_VIOLATED!>convert<!>(l, <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>HashSet<!>())
    checkSubtype<Int>(<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>r<!>)
}
