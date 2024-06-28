// FIR_IDENTICAL
private enum class Foo { A, B }

class Bar(<!EXPOSED_PARAMETER_TYPE!>val foo: Foo<!>)