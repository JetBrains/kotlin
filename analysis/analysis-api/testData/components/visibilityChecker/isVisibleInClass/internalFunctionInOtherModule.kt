// MODULE: dependency
// FILE: Owner.kt
class Owner {
    internal fun member(): Int = 42
}

// MODULE: main(dependency)
// FILE: main.kt
class <caret>Unrelated

// callable: /Owner.member
