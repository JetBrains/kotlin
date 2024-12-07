// TARGET_BACKEND: NATIVE
// NATIVE_STANDALONE

// Purpose of this test is to check that Kotlin/Native still generates correct
// package names in reflection information without package patching.
//
// Native test engine by default groups tests to single compilation, and
// renames packages to avoid clashes.
// To make reflection-related tests work, it inserts ReflectionPackageName
// annotations when patching, to instruct the compiler to use the original
// package name for reflection information.
//
// The NATIVE_STANDALONE directive above disables test grouping.
// So this test basically checks that without ReflectionPackageName the compiler
// can still properly handle reflection information for classes and generates
// correct package names.

// FILE: pkg.kt
package pkg

class A

// FILE: pkg.sub.kt
package pkg.sub

class B

// FILE: defaut.kt
import pkg.*
import pkg.sub.*

class C

fun box(): String {
    if (A::class.qualifiedName != "pkg.A") return "FAIL 1: ${A::class.qualifiedName}"
    if (B::class.qualifiedName != "pkg.sub.B") return "FAIL 2: ${B::class.qualifiedName}"
    if (C::class.qualifiedName != "C") return "FAIL 3: ${C::class.qualifiedName}"

    return "OK"
}
