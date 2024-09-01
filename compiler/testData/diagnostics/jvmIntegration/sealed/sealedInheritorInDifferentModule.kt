// FIR_IDENTICAL
// MODULE: library
// FILE: base.kt
package a

sealed class Base

sealed interface IBase

class A : Base(), IBase

// MODULE: main(library)
// FILE: main.kt
// ISSUE: KT-20423

package a

class B : <!SEALED_INHERITOR_IN_DIFFERENT_MODULE!>Base<!>(), <!SEALED_INHERITOR_IN_DIFFERENT_MODULE!>IBase<!>
