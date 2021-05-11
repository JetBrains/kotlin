// "Import" "true"
// ERROR: Unresolved reference: Some
package p1

import p2.receiveSomeCtor

fun a() {
    receiveSomeCtor(::Some<caret>)
}
/* IGNORE_FIR */