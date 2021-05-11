// "Import" "true"
// ERROR: Unresolved reference: ext

import dep.TA1
import dep.ext

fun use() {
    val ta = TA1()
    ta.ext<caret>
}
/* IGNORE_FIR */