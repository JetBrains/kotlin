// MODULE: dependency
// FILE: Owner.kt
class Owner {
    internal fun member(): Int = 42
}

// MODULE: main()(dependency)
// FILE: main.kt
class <caret>Unrelated {
    fun useSite(owner: Owner): Int = owner.member()
}

// callable: /Owner.member
