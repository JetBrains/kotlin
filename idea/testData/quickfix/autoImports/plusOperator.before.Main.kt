// "Import" "true"
// ERROR: <html>Unresolved reference. <br/> None of the following candidates is applicable because of receiver type mismatch: <ul><li><b>public</b> <b>fun</b> jet.String?.plus(other: jet.Any?): jet.String <i>defined in</i> jet</li></ul></html>

trait H

fun f(h: H?) {
    h <caret>+ "other"
}