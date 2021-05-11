// "Import" "true"
// ERROR: Unresolved reference: someFun
package testingExtensionFunctionsImport

import testingExtensionFunctionsImport.data.someFun

fun some() {
    val str = ""
    str.someFun()
}

/* IGNORE_FIR */