// ISSUE: KT-20423
// !LANGUAGE: +SealedInterfaces +AllowSealedInheritorsInDifferentFilesOfSamePackage

// MODULE: m1
// FILE: a.kt
package a

sealed interface Base

interface A : Base

// MODULE: m2(m1)
// FILE: b.kt

package a

interface B : Base
