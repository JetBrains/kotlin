// "Import" "true"
// ERROR: Unresolved reference: ext

import dep.TA

fun use() {
    val ta = TA()
    ta.ext<caret>()
}
/* IGNORE_FIR */