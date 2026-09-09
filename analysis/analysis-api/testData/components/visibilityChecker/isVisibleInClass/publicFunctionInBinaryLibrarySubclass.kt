// MODULE: dependency
// MODULE_KIND: LibraryBinary
// FILE: Owner.kt
open class Owner {
    fun member(): Int = 42
}

// MODULE: main(dependency)
// FILE: main.kt
class <caret>Child : Owner() {
    fun useSite(): Int = member()
}

// callable: /Owner.member
