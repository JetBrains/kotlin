// "Import" "true"
// ERROR: <html>Unresolved reference. <br/> None of the following candidates is applicable because of receiver type mismatch: <ul><li><b>public</b> operator <b>fun</b> h.A.unaryPlus(): kotlin.Int <i>defined in</i> h</li></ul></html>

package h

import util.unaryPlus

interface H

fun f(h: H?) {
    +h
}

class A()

operator fun A.unaryPlus(): Int = 3