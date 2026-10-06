package test

open class `Hello$World` {
    class Nested
}

class Outer {
    class Inner
}

data class GreetingContainer(
    val hello: `Hello$World`,
    val nestedInDollar: `Hello$World`.Nested,
    val regularNested: Outer.Inner,
    val list: List<`Hello$World`>,
)

class Derived : `Hello$World`() {
    fun take(p: `Hello$World`): `Hello$World` = p
}
