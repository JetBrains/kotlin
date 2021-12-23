private enum class Foo { A, B }

class Bar(<!EXPOSED_PARAMETER_TYPE!>val <!EXPOSED_PROPERTY_TYPE_IN_CONSTRUCTOR_ERROR!>foo<!>: Foo<!>)
