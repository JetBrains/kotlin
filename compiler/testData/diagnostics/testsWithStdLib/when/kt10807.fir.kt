import java.util.*
import kotlin.comparisons.compareBy
import kotlin.comparisons.nullsLast

class Foo(val a: String, val b: Int)

fun getComp(): Comparator<Foo?> =
        when {
            else -> nullsLast(compareBy({ it.<!UNRESOLVED_REFERENCE!>a<!> }, { it.<!UNRESOLVED_REFERENCE!>b<!> }))
        }

fun getCompInverted(): Comparator<Foo?> =
        nullsLast(
                when {
                    else -> compareBy({ it.<!UNRESOLVED_REFERENCE!>a<!> }, { it.<!UNRESOLVED_REFERENCE!>b<!> })
                }
        )