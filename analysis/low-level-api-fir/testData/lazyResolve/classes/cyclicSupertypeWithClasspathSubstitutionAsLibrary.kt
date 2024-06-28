// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// MODULE: dependency
// MODULE_KIND: LibraryBinary
// FILE: dependency.kt
interface ToSubstitute

interface Intermediate : ToSubstitute

// MODULE: main(dependency)
// FILE: main.kt
interface ToSubstitute : Main

interface Ma<caret>in : Intermediate
