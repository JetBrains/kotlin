// "Import" "true"
// ERROR: <html>Unresolved reference. <br/> None of the following candidates is applicable because of receiver type mismatch: <ul><li><b>internal</b> <b>fun</b> jet.String.minus(str: jet.String): jet.String <i>defined in</i> root package</li><li><b>internal</b> <b>fun</b> jet.String.minus(i: java.lang.Integer): jet.String <i>defined in</i> root package</li></ul></html>

trait H

fun f(h: H?) {
    h <caret>- "other"
}


fun String.minus(str: String) = ""

fun String.minus(i: Integer) = ""