package a

import java.util.Date
import java.util.Comparator


fun foo() {

    val <!UNUSED_VARIABLE!>c<!>: Comparator<Date?> = comparator {(date1, date2) ->
        if (date1 != null && date2 != null) {
            date1.compareTo(date2) * -11
        } else {
            11
        }
    }
}

fun bar(i: Int, a: <!UNRESOLVED_REFERENCE!>U<!>) {
    val r = if (true) i else <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>a<!>
    val <!UNUSED_VARIABLE!>b<!>: Any = <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>r<!>
}

//from standard library
public inline fun <T> comparator(<!UNUSED_PARAMETER!>fn<!>: (T,T) -> Int): Comparator<T> {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>