// FIR_IDENTICAL
// JDK_KIND: FULL_JDK_11
// MODULE: main
// FILE: test.kt
fun main() {
    // Module java.naming
    val b: javax.naming.Binding? = null
    println(b)

    // Module java.logging
    val j: java.util.logging.Filter? = null
    println(j)

    // Module java.desktop
    val s: javax.swing.JFrame? = null
    println(s)
}
