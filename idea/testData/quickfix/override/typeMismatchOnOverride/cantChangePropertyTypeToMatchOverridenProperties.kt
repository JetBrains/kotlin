// "class org.jetbrains.kotlin.idea.quickfix.ChangeVariableTypeFix" "false"
// ERROR: <html>Var-property type is 'kotlin.Int', which is not a type of overridden<br/><b>internal</b> <b>abstract</b> <b>var</b> x: kotlin.String <i>defined in</i> A</html>
interface A {
    var x: String
}

interface B {
    var x: Any
}

interface C : A, B {
    override var x: Int<caret>
}
