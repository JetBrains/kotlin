import java.util.*
import kotlin.comparisons.compareBy
import kotlin.comparisons.nullsLast

class Foo(val a: String, val b: Int)

fun getComp(): Comparator<Foo?> =
        when {
            else -> <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>nullsLast<!>(compareBy({ it.<!UNRESOLVED_REFERENCE!>a<!> }, { it.<!UNRESOLVED_REFERENCE!>b<!> }))
        }

fun getCompInverted(): Comparator<Foo?> =
        nullsLast(
                when {
                    else -> <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>compareBy<!>({ it.<!UNRESOLVED_REFERENCE!>a<!> }, { it.<!UNRESOLVED_REFERENCE!>b<!> })
                }
        )
