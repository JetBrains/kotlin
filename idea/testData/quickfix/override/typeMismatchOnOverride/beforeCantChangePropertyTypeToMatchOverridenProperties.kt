// "Change 'C.x' type to 'String'" "false"
// ERROR: <html>Var-property type is 'jet.Int', which is not a type of overridden<br/><b>internal</b> <b>abstract</b> <b>var</b> x: jet.String <i>defined in</i> A</html>
trait A {
    var x: String
}

trait B {
    var x: Any
}

trait C : A, B {
    override var x: Int<caret>
}