// IS_APPLICABLE: false
// WITH_RUNTIME
// ERROR: <html>None of the following functions can be called with the arguments supplied. <ul><li>assert(<font color=red><b>Boolean</b></font>, () &rarr; Any) <i>defined in</i> kotlin</li><li>assert(<font color=red><b>Boolean</b></font>, Any = ...) <i>defined in</i> kotlin</li></ul></html>

fun foo() {
    <caret>assert { "text" }
}