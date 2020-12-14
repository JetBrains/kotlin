// ISSUE: KT-20423
// !LANGUAGE: +SealedInterfaces +AllowSealedInheritorsInDifferentFilesOfSamePackage

// MODULE: m1
// FILE: a.kt
package a

sealed class Base

class A : Base()

// MODULE: m2(m1)
// FILE: b.kt

package a

class B : <!HIDDEN!>Base<!>()
