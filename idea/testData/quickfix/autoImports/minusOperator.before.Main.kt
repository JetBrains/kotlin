// "Import" "true"
// ERROR: <html>Unresolved reference. <br/> None of the following candidates is applicable because of receiver type mismatch: <ul><li><b>internal</b> <b>fun</b> kotlin.String.minus(i: java.lang.Integer): kotlin.String <i>defined in</i> h</li><li><b>internal</b> <b>fun</b> kotlin.String.minus(str: kotlin.String): kotlin.String <i>defined in</i> h</li></ul></html>

package h

interface H

fun f(h: H?) {
    h <caret>- "other"
}


fun String.minus(str: String) = ""

fun String.minus(i: Integer) = ""