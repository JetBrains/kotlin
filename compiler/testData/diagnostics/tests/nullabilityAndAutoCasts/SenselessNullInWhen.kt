//KT-2457 Verify error when comparing not null value with null in when

package kt2457

fun foo(i: Int) : Int =
    when (i) {
        1 -> 1
        <!SENSELESS_NULL_IN_WHEN!>null<!> -> 1
        else -> 1
    }
