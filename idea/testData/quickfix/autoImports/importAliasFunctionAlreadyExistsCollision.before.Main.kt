// "Import" "true"
// ERROR: Unresolved reference: importedFunA

import editor.completion.apx.importedFunA as funA
fun context() {
    fun funA() {}
    <caret>importedFunA()
}
/* IGNORE_FIR */