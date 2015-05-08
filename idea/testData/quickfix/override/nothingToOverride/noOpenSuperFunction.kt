// "class org.jetbrains.kotlin.idea.quickfix.ChangeMemberFunctionSignatureFix" "false"
// ERROR: 'f' overrides nothing
open class A {
    open fun foo() {}
    fun f(a: Int) {}
}

class B : A(){
    <caret>override fun f(a: String) {}
}
