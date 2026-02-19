// FILE: DeclSite.kt

class DeclSite {
    fun calculateX() = 0
}

// FILE: UseSite.kt

fun test(declSite: DeclSite) {
    declSite.<caret>calculateX()
}