// "Import" "true"
// ERROR: Unresolved reference: ext

import dep.A
import dep.ext

fun use() {
    val ta = A()
    ta.ext<caret>
}
/* IGNORE_FIR */