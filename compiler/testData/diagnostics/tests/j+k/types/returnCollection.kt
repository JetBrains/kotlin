// RUN_PIPELINE_TILL: CODEGEN
// FILE: k.kt

interface K {
    fun foo(): List<String>
}

// FILE: J.java

import java.util.*;

interface J extends K {
    List<String> foo();
}

/* GENERATED_FIR_TAGS: functionDeclaration, interfaceDeclaration */
