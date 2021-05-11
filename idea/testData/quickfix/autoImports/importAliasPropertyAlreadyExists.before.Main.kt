// "Import" "true"
// ERROR: Unresolved reference: importedValA

import editor.completion.apx.importedValA as valA
fun context() {
    <caret>importedValA()
}
/* IGNORE_FIR */