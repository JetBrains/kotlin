// "Import" "true"
// ERROR: Unresolved reference. None of the following candidates is applicable because of receiver type mismatch: <br>public operator fun String?.plus(other: Any?): String defined in kotlin

package h

import util.plus

interface H

fun f(h: H?) {
    h <caret>+ "other"
}
/* IGNORE_FIR */