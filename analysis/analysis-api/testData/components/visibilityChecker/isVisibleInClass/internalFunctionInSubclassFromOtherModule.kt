// MODULE: dependency
// FILE: Owner.kt
open class Owner {
    internal fun member(): Int = 42
}

// MODULE: main(dependency)
// FILE: main.kt
class <caret>Child : Owner()

// callable: /Owner.member
