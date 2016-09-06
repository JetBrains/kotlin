class Outer private constructor(public val x: String) {
    class Nested {
        fun foo() = OuterAlias("OK")
    }
}

typealias OuterAlias = Outer

fun box(): String =
        Outer.Nested().foo().x