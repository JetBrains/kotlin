// "Import" "true"
// ERROR: Unresolved reference: Date

import java.util.*
import dependency.*
import java.util.Date

fun foo(d: Date<caret>) {
}

/* IGNORE_FIR */