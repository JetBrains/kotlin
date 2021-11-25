// WITH_STDLIB

import kotlin.collections.forEach as forEach1

fun foo() {
    <!UNRESOLVED_REFERENCE!>z<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>a<!>.<!OVERLOAD_RESOLUTION_AMBIGUITY!>forEach1<!> {  }
}