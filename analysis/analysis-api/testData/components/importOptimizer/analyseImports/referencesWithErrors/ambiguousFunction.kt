// FILE: main.kt
package test

fun test() {
    withOverloads()
}

// FILE: dependency.kt
package dependency

fun withOverloads(i: Int) {}
fun withOverloads(i: String) {}
