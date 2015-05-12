// "Import" "true"
// ERROR: <html>Unresolved reference. <br/> None of the following candidates is applicable because of receiver type mismatch: <ul><li><b>internal</b> <b>fun</b> h.A.plus(): kotlin.Int <i>defined in</i> h</li><li><b>public</b> <b>fun</b> kotlin.String?.plus(other: kotlin.Any?): kotlin.String <i>defined in</i> kotlin</li></ul></html>

package h

interface H

fun f(h: H?) {
    <caret>+h
}

class A()

fun A.plus(): Int = 3