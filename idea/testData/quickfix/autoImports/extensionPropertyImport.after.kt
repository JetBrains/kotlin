// "Import" "true"
// ERROR: Unresolved reference: someVal
package test

import test.data.someVal

fun some() {
    "".someVal
}

/* IGNORE_FIR */