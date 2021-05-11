// "Import" "true"
// ERROR: Unresolved reference: ImportedClass

import editor.completion.apx.ImportedClass as Class2
fun context() {
    class Class2
    val c: editor.completion.apx.ImportedClass
}
/* IGNORE_FIR */