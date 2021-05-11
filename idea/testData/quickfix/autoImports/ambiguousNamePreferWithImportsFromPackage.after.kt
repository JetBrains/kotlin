// "Import" "true"
// ERROR: Unresolved reference: XXX

import dependency2.XXX
import dependency2.YYY

fun foo(x: XXX<caret>) {
}

/* IGNORE_FIR */