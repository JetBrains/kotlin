// "Import" "true"
// ERROR: Unresolved reference. None of the following candidates is applicable because of receiver type mismatch: <br>public fun kotlin.String.minus(i: java.lang.Integer): kotlin.String defined in h<br>public fun kotlin.String.minus(str: kotlin.String): kotlin.String defined in h

package h

import util.minus

interface H

fun f(h: H?) {
    h <caret>- "other"
}


fun String.minus(str: String) = ""

fun String.minus(i: Integer) = ""