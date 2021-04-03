import java.util.*
import kotlin.comparisons.compareBy
import kotlin.comparisons.nullsLast

class Foo(val a: String, val b: Int)

// Strange behaviour: type of the return expression is Comparatot<Any?>, but it should be
// Comparator<Foo?>
fun getComp(): Comparator<Foo?> =
        <!RETURN_TYPE_MISMATCH!>when {
            else -> nullsLast(compareBy({ it.<!UNRESOLVED_REFERENCE!>a<!> }, { it.<!UNRESOLVED_REFERENCE!>b<!> }))
        }<!>

fun getCompInverted(): Comparator<Foo?> =
        nullsLast(
                when {
                    else -> compareBy({ it.<!UNRESOLVED_REFERENCE!>a<!> }, { it.<!UNRESOLVED_REFERENCE!>b<!> })
                }
        )