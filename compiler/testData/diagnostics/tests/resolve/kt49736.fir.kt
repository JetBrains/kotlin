// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

import kotlin.collections.forEach as forEach1

fun foo() {
    <!UNRESOLVED_REFERENCE!>z<!>.a.forEach1 {  }
}