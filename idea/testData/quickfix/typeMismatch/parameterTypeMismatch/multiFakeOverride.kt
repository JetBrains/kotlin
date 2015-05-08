// "class org.jetbrains.kotlin.idea.quickfix.ChangeParameterTypeFix" "false"
// ERROR: <html>Type mismatch.<table><tr><td>Required:</td><td>kotlin.Int</td></tr><tr><td>Found:</td><td>kotlin.String</td></tr></table></html>
trait A {
    fun f(i: Int): Boolean
}

open class AA {
    fun f(i: Int) = true
}

class AAA: AA(), A

val c = AAA().f("<caret>")
