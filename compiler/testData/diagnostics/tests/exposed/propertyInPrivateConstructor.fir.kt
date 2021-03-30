private enum class Foo { A, B }

class Bar private constructor(val <!EXPOSED_PROPERTY_TYPE_IN_CONSTRUCTOR!>foo<!>: Foo)
