// "Import" "true"
// ERROR: <html>Unresolved reference. <br/> None of the following candidates is applicable because of receiver type mismatch: <ul><li><b>internal</b> <b>fun</b> A.plus(): jet.Int <i>defined in</i> root package</li><li><b>public</b> <b>fun</b> jet.String?.plus(other: jet.Any?): jet.String <i>defined in</i> jet</li></ul></html>

trait H

fun f(h: H?) {
    <caret>+h
}

class A()

fun A.plus(): Int = 3