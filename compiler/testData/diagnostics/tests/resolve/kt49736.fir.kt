// WITH_RUNTIME

import kotlin.collections.forEach as forEach1

fun foo() {
    <!UNRESOLVED_REFERENCE!>z<!>.a.forEach1 {  }
}