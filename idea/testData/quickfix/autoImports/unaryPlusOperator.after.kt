// "Import" "true"
// ERROR: <html>Unresolved reference. <br/> None of the following candidates is applicable because of receiver type mismatch: <ul><li><b>public</b> operator <b>fun</b> h.A.plus(): kotlin.Int <i>defined in</i> h</li><li><b>public</b> operator <b>fun</b> kotlin.String?.plus(other: kotlin.Any?): kotlin.String <i>defined in</i> kotlin</li></ul></html>

package h

import util.plus

interface H

fun f(h: H?) {
    +h
}

class A()

operator fun A.plus(): Int = 3