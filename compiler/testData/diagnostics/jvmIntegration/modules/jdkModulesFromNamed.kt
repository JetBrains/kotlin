// JDK_KIND: FULL_JDK_11
// MODULE: main
// FILE: module-info.java
module main {
    requires java.naming;
    requires java.logging;

    requires kotlin.stdlib;
}

// FILE: test.kt
fun main() {
    // Module java.naming
    val b: javax.naming.Binding? = null
    println(b)

    // Module java.logging
    val j: java.util.logging.Filter? = null
    println(j)

    // Module java.desktop (this module doesn't depend on it)
    val s: javax.<!UNRESOLVED_REFERENCE!>swing<!>.JFrame? = null
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>println<!>(s)
}
