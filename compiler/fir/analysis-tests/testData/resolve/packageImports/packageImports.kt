// RUN_PIPELINE_TILL: FRONTEND
// FILE: a.kt

package my.long.library.name

fun hello() { }

// FILE: b.kt

package other.place

import package my.long.library.name

fun test() {
    <!UNRESOLVED_REFERENCE!>hello<!>()
    name.hello()
    my.long.library.name.hello()
}

/* GENERATED_FIR_TAGS: functionDeclaration */
