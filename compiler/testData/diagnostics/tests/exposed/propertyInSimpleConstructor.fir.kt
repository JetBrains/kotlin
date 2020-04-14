private enum class Foo { A, B }

class Bar(<!EXPOSED_PARAMETER_TYPE, EXPOSED_PROPERTY_TYPE!>val foo: Foo<!>)