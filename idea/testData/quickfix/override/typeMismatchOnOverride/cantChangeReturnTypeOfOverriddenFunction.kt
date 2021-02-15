// "class org.jetbrains.kotlin.idea.quickfix.ChangeCallableReturnTypeFix" "false"
// ERROR: Return type of 'foo' is not a subtype of the return type of the overridden member 'public abstract fun foo(): Int defined in A'
interface A {
    fun foo(): Int
}

interface B {
    fun foo(): String
}

interface C : A, B {
    override fun foo(): <caret>Long
}

/* FIR_COMPARISON */
