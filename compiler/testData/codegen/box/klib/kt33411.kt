// MODULE: m1
// FILE: m1.kt
fun f() {}
fun getO() = "O"

// MODULE: m2
// FILE: m2.kt
fun f() {}
fun getK() = "K"

// MODULE: main(m1)(m2)
// FILE: main.kt
fun box() = getO() + getK()
