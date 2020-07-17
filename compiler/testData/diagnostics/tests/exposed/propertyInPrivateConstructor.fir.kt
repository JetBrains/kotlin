private enum class Foo { A, B }

class Bar private constructor(val <!EXPOSED_PROPERTY_TYPE!>foo<!>: Foo)