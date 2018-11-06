private enum class Foo { A, B }

class Bar private constructor(<!EXPOSED_PROPERTY_TYPE_IN_CONSTRUCTOR!>val foo: Foo<!>)