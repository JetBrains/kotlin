// WITH_STDLIB
// MODULE: main
// FILE: module-info.java
module main {
    requires kotlin.stdlib;
}

// FILE: usage.kt
fun usage() {
    Regex("a")
    listOf(1)
}
