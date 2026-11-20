// LL_FIR_DIVERGENCE
// The LL test platform doesn't build the Java module graph from source `module-info.java` files: all default JDK
// modules stay visible, so `javax.swing` from `java.desktop`, which is not required, is resolved.
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: FRONTEND
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
    val s: javax.swing.JFrame? = null
    println(s)
}

/* GENERATED_FIR_TAGS: functionDeclaration, localProperty, nullableType, propertyDeclaration */
