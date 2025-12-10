// MODULE: base
// FILE: base.kt

package base

interface FirCallableDeclaration

val <D : FirCallableDeclaration> D.originalForSubstitutionOverride: D?
    get() = null


// MODULE: context(base)
// COMPILATION_ERRORS
// ^LibrarySource mode doesn't support module dependencies
// FILE: context.kt

package impl

import base.*

fun main() {
    val x = object : FirCallableDeclaration {}
    x.originalIfFakeOverride()
}

inline fun <reified D : FirCallableDeclaration> D.originalIfFakeOverride(): D? =
    <caret_context>originalForSubstitutionOverride

// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: EXPRESSION
originalForSubstitutionOverride
