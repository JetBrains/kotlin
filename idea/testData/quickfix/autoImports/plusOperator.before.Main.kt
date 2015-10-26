// "Import" "true"
// ERROR: Unresolved reference. None of the following candidates is applicable because of receiver type mismatch: <br>public operator fun kotlin.String?.plus(other: kotlin.Any?): kotlin.String defined in kotlin

package h

interface H

fun f(h: H?) {
    h <caret>+ "other"
}