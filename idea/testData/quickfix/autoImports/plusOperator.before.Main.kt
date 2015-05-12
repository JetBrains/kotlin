// "Import" "true"
// ERROR: <html>Unresolved reference. <br/> None of the following candidates is applicable because of receiver type mismatch: <ul><li><b>public</b> <b>fun</b> kotlin.String?.plus(other: kotlin.Any?): kotlin.String <i>defined in</i> kotlin</li></ul></html>

package h

interface H

fun f(h: H?) {
    h <caret>+ "other"
}