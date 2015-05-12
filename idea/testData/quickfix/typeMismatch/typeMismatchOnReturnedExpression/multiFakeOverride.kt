// "Change 'AA.f' function return type to 'Boolean'" "false"
// ACTION: Change 'AAA.g' function return type to 'Int'
// ACTION: Convert to expression body
// ACTION: Disable 'Convert to Expression Body'
// ACTION: Edit intention settings
// ERROR: <html>Type mismatch.<table><tr><td>Required:</td><td>kotlin.Boolean</td></tr><tr><td>Found:</td><td>kotlin.Int</td></tr></table></html>
interface A {
    fun f(): Int
}

open class AA {
    fun f() = 3
}

class AAA: AA(), A {
    fun g(): Boolean {
        return f<caret>()
    }
}