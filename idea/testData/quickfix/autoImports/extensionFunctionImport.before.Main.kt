// "Import" "true"
// ERROR: Unresolved reference: someFun
package testingExtensionFunctionsImport

fun some() {
    val str = ""
    str.<caret>someFun()
}

/* IGNORE_FIR */