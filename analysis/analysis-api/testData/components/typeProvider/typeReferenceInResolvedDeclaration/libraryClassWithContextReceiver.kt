// IGNORE_STANDALONE
// Standalone mode doesn't provide PSI for declarations from binary libraries
// LANGUAGE: +ContextReceivers, -ContextParameters
// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: lib.kt
package lib

class A

context(A)
class Foo

// MODULE: main(lib)
// FILE: main.kt
import lib.Foo

fun test(foo: F<caret>oo) {}
