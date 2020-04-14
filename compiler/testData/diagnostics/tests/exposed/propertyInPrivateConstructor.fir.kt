private enum class Foo { A, B }

class Bar private constructor(<!EXPOSED_PROPERTY_TYPE!>val foo: Foo<!>)