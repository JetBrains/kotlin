// "Import" "true"
// ERROR: Unresolved reference: importedFunA

import editor.completion.apx.importedFunA as funA
fun context() {
    val funA = 42
    funA()
}
/* IGNORE_FIR */