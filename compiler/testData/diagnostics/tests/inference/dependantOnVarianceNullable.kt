package b
//+JDK

import java.util.*
import java.util.Collections.*

fun foo(list: List<String>) : String {
    val w : String = max(list, comparator<String?> {<!UNUSED_ANONYMOUS_PARAMETER!>o1<!>, <!UNUSED_ANONYMOUS_PARAMETER!>o2<!> -> 1
    })
    return w
}

//from library
fun <T> comparator(<!UNUSED_PARAMETER!>fn<!>: (T,T) -> Int): Comparator<T> {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
