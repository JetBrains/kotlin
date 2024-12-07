// FILE: DeclSite.kt

class DeclSite {
    val x = 0
}

// FILE: UseSite.kt

fun test(declSite: DeclSite) {
    declSite.<caret>x
}