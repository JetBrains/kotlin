// IS_APPLICABLE: false
// ERROR: <html>None of the following functions can be called with the arguments supplied. <ul><li>foo(String, Boolean, <font color=red><b>Char</b></font>) <i>defined in</i> root package</li><li>foo(String, Boolean, <font color=red><b>Int</b></font>) <i>defined in</i> root package</li></ul></html>
fun foo(s: String, b: Boolean, p: Int){}
fun foo(s: String, b: Boolean, c: Char){}

fun bar() {
    foo("", <caret>true)
}