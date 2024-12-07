// FILE: DeclSite.kt

class DeclSite {
    var x = 0
}

// FILE: UseSite.kt

fun test(declSite: DeclSite) {
    declSite.<caret>x = 0
}