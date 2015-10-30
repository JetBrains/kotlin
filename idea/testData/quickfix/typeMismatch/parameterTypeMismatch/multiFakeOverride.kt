// "class org.jetbrains.kotlin.idea.quickfix.ChangeParameterTypeFix" "false"
// ERROR: Type mismatch: inferred type is kotlin.String but kotlin.Int was expected
interface A {
    fun f(i: Int): Boolean
}

open class AA {
    fun f(i: Int) = true
}

class AAA: AA(), A

val c = AAA().f("<caret>")
