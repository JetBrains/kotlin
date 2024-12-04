// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// MODULE: dependency
// FILE: dependency.kt
interface ToSubstitute

interface Intermediate : ToSubstitute

// MODULE: main(dependency)
// FILE: main.kt
interface ToSubstitute : Main

interface M<caret>ain : Intermediate
