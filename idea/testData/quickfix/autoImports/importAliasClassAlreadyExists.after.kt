// "Import" "true"
// ERROR: Unresolved reference: ImportedClass

import editor.completion.apx.ImportedClass as Class2
fun context() {
    val c: Class2
}
/* IGNORE_FIR */