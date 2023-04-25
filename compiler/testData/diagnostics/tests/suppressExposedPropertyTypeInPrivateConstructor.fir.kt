// ISSUE: KT-57458

private enum class Foo { A, B }

class Bar private constructor(
    @Suppress("EXPOSED_PROPERTY_TYPE_IN_CONSTRUCTOR_ERROR")
    val <!EXPOSED_PROPERTY_TYPE_IN_CONSTRUCTOR_ERROR!>foo<!>: Foo,
)

class Var private constructor(
    @property:Suppress("EXPOSED_PROPERTY_TYPE_IN_CONSTRUCTOR_ERROR")
    val foo: Foo,
)

class Zar private constructor(
    @param:Suppress("EXPOSED_PROPERTY_TYPE_IN_CONSTRUCTOR_ERROR")
    val <!EXPOSED_PROPERTY_TYPE_IN_CONSTRUCTOR_ERROR!>foo<!>: Foo,
)
