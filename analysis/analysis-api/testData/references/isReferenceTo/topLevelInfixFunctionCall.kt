// FILE: DeclSite.kt

infix fun Int.customPlus(other: Int) = this + other

// FILE: UseSite.kt

fun test() {
    "1" <caret>+ "2"
}