import java.util.*
import kotlin.comparisons.compareBy
import kotlin.comparisons.nullsLast

class Foo(val a: String, val b: Int)

fun getComp(): Comparator<Foo?> =
        <!RETURN_TYPE_MISMATCH!>when {
            else -> nullsLast(compareBy({ it.<!UNRESOLVED_REFERENCE!>a<!> }, { it.<!UNRESOLVED_REFERENCE!>b<!> }))
        }<!>

fun getCompInverted(): Comparator<Foo?> =
        <!TYPE_MISMATCH!>nullsLast(
                when {
                    else -> compareBy({ it.<!UNRESOLVED_REFERENCE!>a<!> }, { it.<!UNRESOLVED_REFERENCE!>b<!> })
                }
        )<!>
