// ISSUE: KT-57458

private enum class Foo { A, B }

class Bar constructor(
    <!EXPOSED_PARAMETER_TYPE!>@Suppress("EXPOSED_PROPERTY_TYPE_IN_CONSTRUCTOR_ERROR")
    val foo: Foo<!>,
)

class Var constructor(
    <!EXPOSED_PARAMETER_TYPE!>@property:Suppress("EXPOSED_PROPERTY_TYPE_IN_CONSTRUCTOR_ERROR")
    val foo: Foo<!>,
)

class Zar constructor(
    <!EXPOSED_PARAMETER_TYPE!>@param:Suppress("EXPOSED_PROPERTY_TYPE_IN_CONSTRUCTOR_ERROR")
    val foo: Foo<!>,
)
