// "Import" "true"
// ERROR: Unresolved reference: importedFunA

import editor.completion.apx.importedFunA as funA
fun context() {
    fun funA() {}
    editor.completion.apx.importedFunA()
}
/* IGNORE_FIR */