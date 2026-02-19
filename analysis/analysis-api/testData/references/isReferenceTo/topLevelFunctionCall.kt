// FILE: DeclSite.kt

fun calculateX() = 0

// FILE: UseSite.kt

fun test() {
    <caret>calculateX()
}