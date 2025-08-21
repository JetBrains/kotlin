// FILE: Outer.kt
annotation class Outer(
    val some: String,
    val nested: foo.Nested,
)

// FILE: Nested.kt
package foo

annotation class Nested(
    val i: Int,
    val s: String,
)

// FILE: WithNested.kt
import foo.Nested
@Outer("value", nested = Nested(0, "nested value"))
class WithNested

// FILE: WithQualifiedNested.kt
@Outer("value", foo.Nested(1, "nested value"))
class WithQualifiedNested
